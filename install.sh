#!/bin/sh

# Magnum Install Script
# Builds locally if in git repo, otherwise downloads from GitHub releases

set -e

# Colors
PRIMARY='\033[38;2;176;46;12m'
GREEN='\033[38;2;76;175;80m'
GRAY='\033[38;2;136;136;136m'
WHITE='\033[1;37m'
RED='\033[0;31m'
NC='\033[0m'

# Use printf for POSIX compatibility
log() { printf "%b\n" "$1"; }
log_err() { printf "%b\n" "$1" >&2; }

REPO="abrayall/magnum"
INSTALL_DIR="/usr/local/bin"

# Detect platform and architecture
detect_platform() {
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)

    case "$OS" in
        darwin) OS="darwin" ;;
        linux) OS="linux" ;;
        mingw*|msys*|cygwin*) OS="windows" ;;
        *) log_err "${RED}Unsupported OS: $OS${NC}"; exit 1 ;;
    esac

    case "$ARCH" in
        x86_64) ARCH="amd64" ;;
        amd64) ARCH="amd64" ;;
        arm64) ARCH="arm64" ;;
        aarch64) ARCH="arm64" ;;
        *) log_err "${RED}Unsupported architecture: $ARCH${NC}"; exit 1 ;;
    esac
}

# Check if we're in the magnum git repo
is_in_repo() {
    if [ -f "go.mod" ] && grep -q "magnum" go.mod 2>/dev/null; then
        if [ -f "build.sh" ]; then
            return 0
        fi
    fi
    return 1
}

# Build from source
build_local() {
    log_err "${PRIMARY}Building from source...${NC}"
    ./build.sh >&2

    BINARY=$(ls build/magnum-*-${OS}-${ARCH}* 2>/dev/null | head -1)
    if [ -z "$BINARY" ]; then
        log_err "${RED}No binary found for ${OS}-${ARCH}${NC}"
        exit 1
    fi

    echo "$BINARY"
}

# Download from GitHub releases
download_release() {
    log_err "${PRIMARY}Fetching latest release...${NC}"

    # Get latest release tag
    if command -v curl > /dev/null 2>&1; then
        LATEST=$(curl -sL "https://api.github.com/repos/${REPO}/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
    elif command -v wget > /dev/null 2>&1; then
        LATEST=$(wget -qO- "https://api.github.com/repos/${REPO}/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
    else
        log_err "${RED}Error: curl or wget is required${NC}"
        exit 1
    fi

    if [ -z "$LATEST" ]; then
        log_err "${RED}Failed to fetch latest release${NC}"
        exit 1
    fi

    log_err "${PRIMARY}Latest version: ${WHITE}${LATEST}${NC}"
    echo "" >&2

    # Construct download URL
    VERSION="${LATEST#v}"
    FILENAME="magnum-${VERSION}-${OS}-${ARCH}"
    if [ "$OS" = "windows" ]; then
        FILENAME="${FILENAME}.exe"
    fi
    URL="https://github.com/${REPO}/releases/download/${LATEST}/${FILENAME}"

    # Create temp directory
    TMP_DIR=$(mktemp -d)
    BINARY="${TMP_DIR}/magnum"

    log_err "${PRIMARY}Downloading ${FILENAME}...${NC}"

    # Download binary
    if command -v curl > /dev/null 2>&1; then
        if ! curl -sL -f -o "$BINARY" "$URL"; then
            log_err "${RED}Failed to download from ${URL}${NC}"
            rm -rf "$TMP_DIR"
            exit 1
        fi
    else
        if ! wget -q -O "$BINARY" "$URL"; then
            log_err "${RED}Failed to download from ${URL}${NC}"
            rm -rf "$TMP_DIR"
            exit 1
        fi
    fi

    chmod +x "$BINARY"
    echo "$BINARY"
}

# Install binary
install_binary() {
    BINARY="$1"

    log "${PRIMARY}Installing to ${INSTALL_DIR}...${NC}"

    if [ -w "$INSTALL_DIR" ]; then
        cp "$BINARY" "$INSTALL_DIR/magnum"
        chmod +x "$INSTALL_DIR/magnum"
    else
        sudo cp "$BINARY" "$INSTALL_DIR/magnum"
        sudo chmod +x "$INSTALL_DIR/magnum"
    fi
}

# Main
detect_platform

if is_in_repo; then
    BINARY=$(build_local)
else
    BINARY=$(download_release)
fi

install_binary "$BINARY"

# Cleanup temp files if downloaded
case "$BINARY" in
    /tmp/*|*/tmp.*) rm -rf "$(dirname "$BINARY")" ;;
esac

echo ""
log "${GREEN}Installed magnum to ${INSTALL_DIR}/magnum${NC}"
echo ""
echo "Run 'magnum --help' to get started"
echo ""
