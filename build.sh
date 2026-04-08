#!/usr/bin/env bash
# Render Build Script
echo "Starting Build..."
mkdir -p out
javac -cp "lib/*" src/SchoolServer.java -d out
echo "Build complete."
