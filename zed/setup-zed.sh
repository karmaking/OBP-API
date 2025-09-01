#!/bin/bash

# Zed IDE Setup Script for OBP-API
# This script copies the recommended Zed configuration to your local .zed folder

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ZED_DIR="$PROJECT_ROOT/.zed"

echo "🔧 Setting up Zed IDE configuration for OBP-API..."

# Create .zed directory if it doesn't exist
if [ ! -d "$ZED_DIR" ]; then
    echo "📁 Creating .zed directory..."
    mkdir -p "$ZED_DIR"
else
    echo "📁 .zed directory already exists"
fi

# Copy settings.json
if [ -f "$SCRIPT_DIR/settings.json" ]; then
    echo "⚙️  Copying settings.json..."
    cp "$SCRIPT_DIR/settings.json" "$ZED_DIR/settings.json"
    echo "✅ settings.json copied successfully"
else
    echo "❌ Error: settings.json not found in zed folder"
    exit 1
fi

# Copy tasks.json
if [ -f "$SCRIPT_DIR/tasks.json" ]; then
    echo "📋 Copying tasks.json..."
    cp "$SCRIPT_DIR/tasks.json" "$ZED_DIR/tasks.json"
    echo "✅ tasks.json copied successfully"
else
    echo "❌ Error: tasks.json not found in zed folder"
    exit 1
fi

echo ""
echo "🎉 Zed IDE setup completed successfully!"
echo ""
echo "Your Zed configuration includes:"
echo "  • Format on save: DISABLED (preserves your code formatting)"
echo "  • Scala/Metals LSP configuration optimized for OBP-API"
echo "  • 9 predefined tasks for building, running, and testing"
echo ""
echo "To see available tasks in Zed, use: Cmd/Ctrl + Shift + P → 'task: spawn'"
echo ""
echo "Note: The .zed folder is in .gitignore, so you can customize settings"
echo "      without affecting other developers."