#!/bin/bash
set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Get script directory and change to it
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Clean and create build directory
rm -rf build
mkdir -p build

# Get version using vermouth
VERSION=$(vermouth 2>/dev/null || curl -sfL https://raw.githubusercontent.com/abrayall/vermouth/refs/heads/main/vermouth.sh | sh -)
echo -e "${GREEN}Building magnum version: ${VERSION}${NC}"
echo ""

# Platforms to build for
PLATFORMS=(
    "darwin/amd64"
    "darwin/arm64"
    "linux/amd64"
    "linux/arm64"
    "windows/amd64"
)

# Build for each platform
for PLATFORM in "${PLATFORMS[@]}"; do
    OS="${PLATFORM%/*}"
    ARCH="${PLATFORM#*/}"

    OUTPUT="magnum-${VERSION}-${OS}-${ARCH}"
    if [ "$OS" = "windows" ]; then
        OUTPUT="${OUTPUT}.exe"
    fi

    echo -e "${YELLOW}Building ${OS}/${ARCH}...${NC}"
    GOOS=$OS GOARCH=$ARCH go build -ldflags "-X main.Version=${VERSION}" -o "build/${OUTPUT}" .
    echo -e "${GREEN}  -> build/${OUTPUT}${NC}"
done

echo ""
echo -e "${GREEN}Build complete!${NC}"
echo ""
ls -la build/
