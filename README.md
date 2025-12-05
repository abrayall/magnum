# Magnum

```
███╗   ███╗ █████╗  ██████╗ ███╗   ██╗██╗   ██╗███╗   ███╗
████╗ ████║██╔══██╗██╔════╝ ████╗  ██║██║   ██║████╗ ████║
██╔████╔██║███████║██║  ███╗██╔██╗ ██║██║   ██║██╔████╔██║
██║╚██╔╝██║██╔══██║██║   ██║██║╚██╗██║██║   ██║██║╚██╔╝██║
██║ ╚═╝ ██║██║  ██║╚██████╔╝██║ ╚████║╚██████╔╝██║ ╚═╝ ██║
╚═╝     ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ ╚═╝     ╚═╝
```

A simple, fast file watcher that runs commands when files change. Built with native OS file system notifications for instant detection.

## Installation

### Quick Install

**macOS/Linux:**
```bash
curl -sfL https://raw.githubusercontent.com/abrayall/magnum/refs/heads/main/install.sh | sh -
```

### Download Binary

Download the latest release for your platform from the [releases page](https://github.com/abrayall/magnum/releases).

### Build from Source

```bash
git clone https://github.com/abrayall/magnum.git
cd magnum
./install.sh
```

## Usage

```bash
magnum [options] <command>
magnum [options] -- <command with args>
```

### Options

| Option | Description |
|--------|-------------|
| `--dir=<path>` | Directory to watch (default: current directory) |
| `--delay=<duration>` | Delay before running command (default: 100ms) |
| `--include=<glob>` | Only watch files matching pattern (comma-separated or repeated) |
| `--exclude=<glob>` | Exclude files matching pattern (comma-separated or repeated) |
| `--verbose` | Show which files changed |
| `--quiet`, `-q` | Suppress all output except command output |
| `--version`, `-v` | Show version |
| `--help`, `-h` | Show help |

### Examples

Watch current directory and run `go build`:
```bash
magnum "go build ./..."
```

Watch only Go files and run tests:
```bash
magnum --include="*.go" -- go test ./...
```

Watch multiple file types:
```bash
magnum --include="*.go,*.html,*.css" "make build"
```

Watch a specific directory, exclude test files:
```bash
magnum --dir=./src --exclude="*_test.go" "npm run build"
```

Use a longer delay to batch rapid changes:
```bash
magnum --delay=500ms "make"
```

Run silently (only show command output):
```bash
magnum -q "go build ./..."
```

### Default Excludes

The following directories are excluded by default:
- Hidden directories (`.*`)
- `node_modules`
- `vendor`
- `build`
- `dist`

## How It Works

Magnum uses native OS file system notifications (via [fsnotify](https://github.com/fsnotify/fsnotify)):
- **macOS**: FSEvents
- **Linux**: inotify
- **Windows**: ReadDirectoryChangesW

This means zero polling and instant detection of file changes. A configurable delay (default 100ms) debounces rapid changes to avoid running the command multiple times for a single save operation.

## Building

```bash
./build.sh
```

This will create binaries for all supported platforms in the `build/` directory:
- `darwin/amd64` (macOS Intel)
- `darwin/arm64` (macOS Apple Silicon)
- `linux/amd64`
- `linux/arm64`
- `windows/amd64`

## Future Work

### `.gitignore` Support
- Automatically respect `.gitignore` patterns
- Option to enable/disable with `--gitignore` flag
- Support for nested `.gitignore` files

### Build Tool Integration
Automatic detection and commands for popular build tools:
- **Go**: `go build`, `go test`
- **Node.js**: `npm run build`, `yarn build`
- **Gradle**: `./gradlew build`
- **Maven**: `mvn compile`
- **Ant**: `ant build`
- **Make**: `make`
- **Grunt**: `grunt`
- **Gulp**: `gulp`
- **Cargo (Rust)**: `cargo build`

Usage would be as simple as:
```bash
magnum --auto
```

### Additional Features
- **Configuration file**: `.magnumrc` or `magnum.yaml` for project-specific settings
- **Multiple commands**: Run different commands for different file patterns
- **Process management**: Kill running command before starting new one (for long-running processes like servers)
- **Notification support**: Desktop notifications on command success/failure
- **Watch depth control**: Limit recursive directory watching depth
- **Regex patterns**: Support regex in addition to glob patterns
- **Ignore file**: `.magnumignore` for project-specific excludes
- **Stats mode**: Show file change statistics and timing information
- **Dry run mode**: Show what would be triggered without running commands
- **Restart mode**: For watching servers - automatically restart on changes
- **Parallel commands**: Run multiple commands concurrently
- **Conditional execution**: Only run command if files matching certain patterns changed
- **Hooks**: Pre and post command hooks
- **Remote sync**: Trigger commands on remote machines via SSH

## License

MIT
