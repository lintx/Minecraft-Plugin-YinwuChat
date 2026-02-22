#!/bin/bash

echo "========================================"
echo "YinwuChat Multi-Platform Build Script"
echo "========================================"
echo

case "$1" in
    "all"|"")
        echo "Building all platforms..."
        mvn clean package -q
        echo
        echo "Build completed! Check target/ directory for JAR files."
        ;;
    "velocity")
        echo "Building Velocity platform..."
        mvn clean package -P velocity -q
        echo
        echo "Velocity JAR created: target/YinwuChat-Velocity-2.12.jar"
        ;;
    "bukkit")
        echo "Building Bukkit platform..."
        mvn clean package -P bukkit -q
        echo
        echo "Bukkit JAR created: target/YinwuChat-Bukkit-2.12.jar"
        ;;
    *)
        echo "Usage: $0 [platform]"
        echo
        echo "Platforms:"
        echo "  all       - Build all platforms (default)"
        echo "  velocity  - Build only Velocity proxy plugin"
        echo "  bukkit    - Build only Bukkit/Spigot plugin"
        echo
        echo "Examples:"
        echo "  $0 all        - Build both Velocity and Bukkit versions"
        echo "  $0 velocity   - Build only Velocity version"
        echo "  $0 bukkit     - Build only Bukkit version"
        echo
        echo "Output files:"
        echo "  - target/YinwuChat-2.12.jar (all platforms)"
        echo "  - target/YinwuChat-Velocity-2.12.jar (Velocity only)"
        echo "  - target/YinwuChat-Bukkit-2.12.jar (Bukkit only)"
        echo
        exit 1
        ;;
esac

echo
echo "Build process completed!"
