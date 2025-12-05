package main

import (
	"fmt"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"strings"
	"syscall"
	"time"

	"github.com/fsnotify/fsnotify"
)

var Version = "development"

const banner = `
███╗   ███╗ █████╗  ██████╗ ███╗   ██╗██╗   ██╗███╗   ███╗
████╗ ████║██╔══██╗██╔════╝ ████╗  ██║██║   ██║████╗ ████║
██╔████╔██║███████║██║  ███╗██╔██╗ ██║██║   ██║██╔████╔██║
██║╚██╔╝██║██╔══██║██║   ██║██║╚██╗██║██║   ██║██║╚██╔╝██║
██║ ╚═╝ ██║██║  ██║╚██████╔╝██║ ╚████║╚██████╔╝██║ ╚═╝ ██║
╚═╝     ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ ╚═╝     ╚═╝`

const divider = "═══════════════════════════════════════════════════════════"

// ANSI color codes
const (
	colorReset   = "\033[0m"
	colorPrimary = "\033[38;2;176;46;12m"  // #B02E0C - header
	colorAccent  = "\033[38;2;220;80;40m"  // brighter red for help sections
	colorGreen   = "\033[38;2;76;175;80m"
	colorRed     = "\033[38;2;244;67;54m"
	colorWhite   = "\033[38;2;255;255;255m"
	colorMuted   = "\033[38;2;128;128;128m"
)

func main() {
	args := os.Args[1:]

	// Parse arguments
	var command string
	var directory string = "."
	var delay time.Duration = 100 * time.Millisecond
	var exclude []string
	var include []string
	var verbose bool
	var quiet bool

	for i := 0; i < len(args); i++ {
		arg := args[i]
		switch {
		case arg == "--version" || arg == "-v":
			fmt.Println(Version)
			return
		case arg == "--help" || arg == "-h":
			printHelp()
			return
		case arg == "--verbose":
			verbose = true
		case arg == "--quiet" || arg == "-q":
			quiet = true
		case strings.HasPrefix(arg, "--dir="):
			directory = strings.TrimPrefix(arg, "--dir=")
		case strings.HasPrefix(arg, "--delay="):
			val := strings.TrimPrefix(arg, "--delay=")
			if d, err := time.ParseDuration(val); err == nil {
				delay = d
			}
		case strings.HasPrefix(arg, "--exclude="):
			val := strings.TrimPrefix(arg, "--exclude=")
			for _, pattern := range strings.Split(val, ",") {
				if p := strings.TrimSpace(pattern); p != "" {
					exclude = append(exclude, p)
				}
			}
		case strings.HasPrefix(arg, "--include="):
			val := strings.TrimPrefix(arg, "--include=")
			for _, pattern := range strings.Split(val, ",") {
				if p := strings.TrimSpace(pattern); p != "" {
					include = append(include, p)
				}
			}
		case arg == "--":
			if i+1 < len(args) {
				command = strings.Join(args[i+1:], " ")
			}
			i = len(args)
		default:
			if command == "" {
				command = arg
			}
		}
	}

	if command == "" {
		printHelp()
		return
	}

	// Convert to absolute path
	absDir, err := filepath.Abs(directory)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	// Print startup message
	if !quiet {
		printHeader()
		fmt.Printf("%sWatching:%s %s\n", colorMuted, colorReset, absDir)
		fmt.Printf("%sCommand:%s  %s\n", colorMuted, colorReset, command)
		if len(include) > 0 {
			fmt.Printf("%sInclude:%s  %s\n", colorMuted, colorReset, strings.Join(include, ", "))
		}
		if len(exclude) > 0 {
			fmt.Printf("%sExclude:%s  %s\n", colorMuted, colorReset, strings.Join(exclude, ", "))
		}
		fmt.Println()
	}

	// Handle Ctrl-C gracefully
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-sigChan
		if !quiet {
			timestamp := time.Now().Format("15:04:05")
			fmt.Printf("\n%s[%s]%s Stopping...\n", colorMuted, timestamp, colorReset)
		}
		os.Exit(0)
	}()

	// Start watching
	if err := watch(absDir, command, delay, include, exclude, verbose, quiet); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
}

func watch(dir, command string, delay time.Duration, include, exclude []string, verbose, quiet bool) error {
	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		return fmt.Errorf("failed to create watcher: %w", err)
	}
	defer watcher.Close()

	// Add directories recursively
	err = filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return nil
		}
		if info.IsDir() {
			name := info.Name()
			// Skip hidden directories and common non-source directories
			if name != "." && strings.HasPrefix(name, ".") {
				return filepath.SkipDir
			}
			if name == "node_modules" || name == "vendor" || name == "build" || name == "dist" {
				return filepath.SkipDir
			}
			// Check exclude patterns for directories
			for _, pattern := range exclude {
				if matched, _ := filepath.Match(pattern, name); matched {
					return filepath.SkipDir
				}
			}
			if err := watcher.Add(path); err != nil {
				if verbose {
					fmt.Fprintf(os.Stderr, "Warning: could not watch %s: %v\n", path, err)
				}
			} else if verbose {
				fmt.Printf("%s[watching]%s %s\n", colorMuted, colorReset, path)
			}
		}
		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to walk directory: %w", err)
	}

	// Run command initially
	runCommand(command, verbose, quiet)

	// Debounce timer
	var timer *time.Timer
	var lastEvent time.Time

	for {
		select {
		case event, ok := <-watcher.Events:
			if !ok {
				return nil
			}

			// Check if file matches filters
			isDelete := event.Op&fsnotify.Remove == fsnotify.Remove || event.Op&fsnotify.Rename == fsnotify.Rename
			if !shouldWatch(event.Name, dir, include, exclude, isDelete) {
				continue
			}

			// Handle new directories
			if event.Op&fsnotify.Create == fsnotify.Create {
				if info, err := os.Stat(event.Name); err == nil && info.IsDir() {
					name := info.Name()
					if !strings.HasPrefix(name, ".") &&
						name != "node_modules" && name != "vendor" &&
						name != "build" && name != "dist" {
						watcher.Add(event.Name)
					}
				}
			}

			if verbose {
				opStr := eventOpString(event.Op)
				fmt.Printf("%s[%s]%s %s\n", colorGreen, opStr, colorReset, event.Name)
			}

			// Delay: reset timer on each event
			lastEvent = time.Now()
			if timer != nil {
				timer.Stop()
			}
			timer = time.AfterFunc(delay, func() {
				if time.Since(lastEvent) >= delay {
					runCommand(command, verbose, quiet)
				}
			})

		case err, ok := <-watcher.Errors:
			if !ok {
				return nil
			}
			if verbose {
				fmt.Fprintf(os.Stderr, "Watcher error: %v\n", err)
			}
		}
	}
}

func shouldWatch(path, baseDir string, include, exclude []string, isDelete bool) bool {
	name := filepath.Base(path)
	relPath, err := filepath.Rel(baseDir, path)
	if err != nil {
		relPath = path
	}

	// For non-delete events, check if it's a directory
	if !isDelete {
		info, err := os.Stat(path)
		if err != nil {
			return false
		}
		// Skip directories for event matching (we still watch them)
		if info.IsDir() {
			return false
		}
	}

	// Check exclude patterns
	for _, pattern := range exclude {
		if matched, _ := filepath.Match(pattern, name); matched {
			return false
		}
		if matched, _ := filepath.Match(pattern, relPath); matched {
			return false
		}
	}

	// Check include patterns (if specified, file must match at least one)
	if len(include) > 0 {
		for _, pattern := range include {
			if matched, _ := filepath.Match(pattern, name); matched {
				return true
			}
			if matched, _ := filepath.Match(pattern, relPath); matched {
				return true
			}
		}
		return false
	}

	return true
}

func eventOpString(op fsnotify.Op) string {
	switch {
	case op&fsnotify.Create == fsnotify.Create:
		return "created"
	case op&fsnotify.Write == fsnotify.Write:
		return "modified"
	case op&fsnotify.Remove == fsnotify.Remove:
		return "deleted"
	case op&fsnotify.Rename == fsnotify.Rename:
		return "renamed"
	case op&fsnotify.Chmod == fsnotify.Chmod:
		return "chmod"
	default:
		return "changed"
	}
}

func runCommand(command string, verbose, quiet bool) {
	timestamp := time.Now().Format("15:04:05")
	if !quiet {
		fmt.Printf("%s[%s]%s Running: %s\n", colorMuted, timestamp, colorReset, command)
	}

	cmd := exec.Command("sh", "-c", command)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin

	startTime := time.Now()
	err := cmd.Run()
	duration := time.Since(startTime)

	if !quiet {
		if err != nil {
			fmt.Printf("%s[%s]%s %sCommand failed%s (%s)\n", colorMuted, timestamp, colorReset, colorRed, colorReset, duration.Round(time.Millisecond))
		} else {
			fmt.Printf("%s[%s]%s %sCommand completed%s (%s)\n", colorMuted, timestamp, colorReset, colorGreen, colorReset, duration.Round(time.Millisecond))
		}
		fmt.Println()
	}
}

func printHeader() {
	fmt.Println()
	fmt.Printf("%s%s%s\n", colorMuted, divider, colorReset)
	fmt.Printf("%s%s%s\n", colorPrimary, banner, colorReset)
	fmt.Printf(" %sv%s%s\n", colorWhite, Version, colorReset)
	fmt.Println()
	fmt.Printf("%s%s%s\n", colorMuted, divider, colorReset)
	fmt.Println()
}

func printHelp() {
	printHeader()
	fmt.Printf("%sFile watcher that runs commands on changes%s\n", colorMuted, colorReset)
	fmt.Println()
	fmt.Printf("%sUsage:%s magnum [options] %s<command>%s\n", colorAccent, colorReset, colorWhite, colorReset)
	fmt.Printf("       magnum [options] %s--%s <command with args>\n", colorWhite, colorReset)
	fmt.Println()
	fmt.Printf("%sOptions:%s\n", colorAccent, colorReset)
	fmt.Printf("  %s--dir%s=<path>       Directory to watch %s(default: current directory)%s\n", colorWhite, colorReset, colorMuted, colorReset)
	fmt.Printf("  %s--delay%s=<duration> Delay before running command %s(default: 100ms)%s\n", colorWhite, colorReset, colorMuted, colorReset)
	fmt.Printf("  %s--include%s=<glob>   Only watch files matching pattern %s(can be repeated)%s\n", colorWhite, colorReset, colorMuted, colorReset)
	fmt.Printf("  %s--exclude%s=<glob>   Exclude files matching pattern %s(can be repeated)%s\n", colorWhite, colorReset, colorMuted, colorReset)
	fmt.Printf("  %s--verbose%s          Show which files changed\n", colorWhite, colorReset)
	fmt.Printf("  %s--quiet%s, %s-q%s        Suppress all output except command output\n", colorWhite, colorReset, colorWhite, colorReset)
	fmt.Printf("  %s--version%s, %s-v%s      Show version\n", colorWhite, colorReset, colorWhite, colorReset)
	fmt.Printf("  %s--help%s, %s-h%s         Show this help\n", colorWhite, colorReset, colorWhite, colorReset)
	fmt.Println()
	fmt.Printf("%sExamples:%s\n", colorAccent, colorReset)
	fmt.Printf("  %smagnum \"go build ./...\"%s\n", colorMuted, colorReset)
	fmt.Printf("  %smagnum --include=\"*.go\" -- go test ./...%s\n", colorMuted, colorReset)
	fmt.Printf("  %smagnum --dir=./src --exclude=\"*_test.go\" \"npm run build\"%s\n", colorMuted, colorReset)
	fmt.Printf("  %smagnum --delay=200ms \"make\"%s\n", colorMuted, colorReset)
	fmt.Println()
	fmt.Printf("%sDefault Excludes:%s\n", colorAccent, colorReset)
	fmt.Printf("  %sHidden directories (.*), node_modules, vendor, build, dist%s\n", colorMuted, colorReset)
	fmt.Println()
}
