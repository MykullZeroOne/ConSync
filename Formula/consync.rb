# Homebrew Formula for ConSync
# To use this formula, you need to create a tap:
# 1. Create a new repository: homebrew-consync
# 2. Place this file in Formula/consync.rb
# 3. Users install with: brew tap yourusername/consync && brew install consync

class Consync < Formula
  desc "Synchronize Markdown documentation with Confluence"
  homepage "https://github.com/yourusername/consync"
  url "https://github.com/yourusername/consync/releases/download/v0.1.0/consync-0.1.0-unix.tar.gz"
  sha256 "REPLACE_WITH_ACTUAL_SHA256"
  version "0.1.0"
  license "MIT"

  depends_on "openjdk@17"

  def install
    # Install JAR to libexec
    libexec.install "consync.jar"

    # Create wrapper script
    (bin/"consync").write <<~EOS
      #!/bin/bash

      # Load credentials if they exist
      if [ -f "$HOME/.consync-app/credentials" ]; then
        source "$HOME/.consync-app/credentials"
      fi

      # Run ConSync
      exec "#{Formula["openjdk@17"].opt_bin}/java" -jar "#{libexec}/consync.jar" "$@"
    EOS

    chmod 0755, bin/"consync"

    # Install documentation
    doc.install "README.md" if File.exist?("README.md")
    doc.install "INSTALL.md" if File.exist?("INSTALL.md")
  end

  def post_install
    # Create credentials directory
    credentials_dir = Pathname.new(Dir.home) / ".consync-app"
    credentials_dir.mkpath

    ohai "ConSync installed successfully!"
    puts ""
    puts "To configure authentication:"
    puts ""
    puts "For Confluence Cloud:"
    puts "  export CONFLUENCE_USERNAME=\"your-email@example.com\""
    puts "  export CONFLUENCE_API_TOKEN=\"your-api-token\""
    puts ""
    puts "For Confluence Data Center/Server:"
    puts "  export CONFLUENCE_PAT=\"your-personal-access-token\""
    puts ""
    puts "Add these to your ~/.zshrc or ~/.bashrc"
    puts ""
    puts "Get started: consync --help"
  end

  def caveats
    <<~EOS
      ConSync requires Java 17 or higher.

      Authentication credentials can be configured in:
        ~/.consync-app/credentials

      Or set environment variables:
        CONFLUENCE_USERNAME and CONFLUENCE_API_TOKEN (Cloud)
        CONFLUENCE_PAT (Data Center/Server)

      Documentation: https://github.com/yourusername/consync
    EOS
  end

  test do
    # Test that the command runs
    assert_match version.to_s, shell_output("#{bin}/consync --version 2>&1", 0)
  end
end
