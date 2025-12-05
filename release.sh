#!/bin/bash

# Magnum GitHub Release Script
# Creates a GitHub release and uploads binaries

set -e

# Colors
GREEN='\033[38;2;76;175;80m'
YELLOW='\033[38;2;222;184;65m'
BLUE='\033[38;2;59;130;246m'
GRAY='\033[38;2;136;136;136m'
RED='\033[0;31m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "=============================================="
echo -e "${YELLOW}Magnum Release${NC}"
echo "=============================================="
echo ""

# Check for GitHub token
if [ -z "$GITHUB_TOKEN" ]; then
    echo -e "${RED}Error: GITHUB_TOKEN environment variable not set${NC}"
    exit 1
fi

# Get version
VERSION=$(vermouth 2>/dev/null || curl -sfL https://raw.githubusercontent.com/abrayall/vermouth/refs/heads/main/vermouth.sh | sh -)

# Check if version matches X.Y.Z format (no pre-release or timestamp)
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Not a release version${NC}"
    echo -e "${GRAY}Current version: $VERSION${NC}"
    echo -e "${GRAY}Release versions must match X.Y.Z format (e.g., 1.0.0)${NC}"
    exit 1
fi

echo -e "${BLUE}Version:${NC} $VERSION"
echo ""

# Get repository info
REPO_URL=$(git config --get remote.origin.url)
if [[ "$REPO_URL" =~ github\.com[:/]([^/]+)/([^/.]+) ]]; then
    OWNER="${BASH_REMATCH[1]}"
    REPO="${BASH_REMATCH[2]}"
else
    echo -e "${RED}Error: Could not parse GitHub repository from remote URL${NC}"
    exit 1
fi

echo -e "${BLUE}Repository:${NC} $OWNER/$REPO"
echo ""

# Delete existing release if it exists
EXISTING_RELEASE=$(curl -s \
    -H "Authorization: Bearer $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "https://api.github.com/repos/$OWNER/$REPO/releases/tags/v$VERSION")

if echo "$EXISTING_RELEASE" | grep -q '"id"'; then
    EXISTING_ID=$(echo "$EXISTING_RELEASE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
    if [ -n "$EXISTING_ID" ]; then
        echo -e "${YELLOW}Deleting existing release v$VERSION...${NC}"
        curl -s -X DELETE \
            -H "Authorization: Bearer $GITHUB_TOKEN" \
            -H "Accept: application/vnd.github+json" \
            -H "X-GitHub-Api-Version: 2022-11-28" \
            "https://api.github.com/repos/$OWNER/$REPO/releases/$EXISTING_ID"
        echo -e "${GREEN}✓ Deleted existing release${NC}"
        echo ""
    fi
fi

# Check if binaries exist
BUILD_DIR="$SCRIPT_DIR/build"
BINARIES=(
    "magnum-${VERSION}-darwin-amd64"
    "magnum-${VERSION}-darwin-arm64"
    "magnum-${VERSION}-linux-amd64"
    "magnum-${VERSION}-linux-arm64"
    "magnum-${VERSION}-windows-amd64.exe"
)

echo -e "${BLUE}Checking for build artifacts...${NC}"
MISSING_ARTIFACTS=0
for BINARY in "${BINARIES[@]}"; do
    if [ ! -f "$BUILD_DIR/$BINARY" ]; then
        echo -e "${RED}✗ Missing: $BINARY${NC}"
        MISSING_ARTIFACTS=1
    else
        echo -e "${GRAY}✓ Found: $BINARY${NC}"
    fi
done

if [ $MISSING_ARTIFACTS -eq 1 ]; then
    echo ""
    echo -e "${RED}Error: Missing artifacts. Run ./build.sh first.${NC}"
    exit 1
fi

echo ""

# Create release
echo -e "${YELLOW}Creating GitHub release...${NC}"

RELEASE_NOTES="Release v$VERSION"

RELEASE_RESPONSE=$(curl -s -X POST \
    -H "Authorization: Bearer $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "https://api.github.com/repos/$OWNER/$REPO/releases" \
    -d "{
        \"tag_name\": \"v$VERSION\",
        \"name\": \"v$VERSION\",
        \"body\": \"$RELEASE_NOTES\",
        \"draft\": false,
        \"prerelease\": false
    }")

RELEASE_ID=$(echo "$RELEASE_RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(data.get('id', ''))
" 2>/dev/null)

if [ -z "$RELEASE_ID" ]; then
    echo -e "${RED}Error: Failed to create release${NC}"
    echo "$RELEASE_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Created release: v$VERSION${NC}"
echo -e "${GRAY}  Release ID: $RELEASE_ID${NC}"
echo ""

# Upload binaries
echo -e "${YELLOW}Uploading binaries...${NC}"

for BINARY in "${BINARIES[@]}"; do
    echo -e "${BLUE}Uploading $BINARY...${NC}"

    UPLOAD_URL="https://uploads.github.com/repos/$OWNER/$REPO/releases/$RELEASE_ID/assets?name=$BINARY"

    UPLOAD_RESPONSE=$(curl -s -X POST \
        -H "Authorization: Bearer $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github+json" \
        -H "Content-Type: application/octet-stream" \
        -H "X-GitHub-Api-Version: 2022-11-28" \
        --data-binary "@$BUILD_DIR/$BINARY" \
        "$UPLOAD_URL")

    ASSET_ID=$(echo "$UPLOAD_RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(data.get('id', ''))
" 2>/dev/null)

    if [ -z "$ASSET_ID" ]; then
        echo -e "${RED}✗ Failed to upload $BINARY${NC}"
    else
        echo -e "${GREEN}✓ Uploaded $BINARY${NC}"
    fi
done

echo ""
echo "=============================================="
echo -e "${GREEN}Release Complete!${NC}"
echo "=============================================="
echo ""
echo -e "${BLUE}Release URL:${NC} https://github.com/$OWNER/$REPO/releases/tag/v$VERSION"
echo ""
