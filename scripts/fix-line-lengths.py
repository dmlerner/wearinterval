#!/usr/bin/env python3
"""
Automatically fix long lines in Kotlin files by breaking them appropriately.
"""
import re
import sys
import os
from pathlib import Path

MAX_LINE_LENGTH = 140

def fix_long_lines(content):
    """Fix long lines in Kotlin code."""
    lines = content.split('\n')
    fixed_lines = []
    
    for line in lines:
        if len(line) <= MAX_LINE_LENGTH:
            fixed_lines.append(line)
            continue
            
        # Get indentation
        indent = len(line) - len(line.lstrip())
        base_indent = ' ' * indent
        
        # Common patterns to fix
        fixed_line = fix_line_patterns(line, base_indent)
        
        if isinstance(fixed_line, list):
            fixed_lines.extend(fixed_line)
        else:
            fixed_lines.append(fixed_line)
    
    return '\n'.join(fixed_lines)

def fix_line_patterns(line, base_indent):
    """Fix specific patterns that commonly cause long lines."""
    
    # Pattern 1: Long Log.d statements
    log_pattern = r'(.*android\.util\.Log\.d\(\s*)"([^"]*)",\s*"([^"]*)"(\s*\).*)'
    match = re.match(log_pattern, line)
    if match:
        prefix, tag, message, suffix = match.groups()
        return [
            f'{prefix}',
            f'{base_indent}    "{tag}",',
            f'{base_indent}    "{message}"{suffix}'
        ]
    
    # Pattern 2: Long function calls with multiple parameters
    func_call_pattern = r'(.*\w+\([^,)]*),\s*([^,)]+),\s*([^)]+\).*)'
    match = re.match(func_call_pattern, line)
    if match and '(' in match.group(1):
        prefix, param1, rest = match.groups()
        return [
            f'{prefix},',
            f'{base_indent}    {param1},',
            f'{base_indent}    {rest}'
        ]
    
    # Pattern 3: Long string concatenations
    concat_pattern = r'(.*")(\s*\+\s*")([^"]*".*)'
    match = re.search(concat_pattern, line)
    if match:
        before, operator, after = match.groups()
        return [
            f'{before}',
            f'{base_indent}    {operator.strip()}{after}'
        ]
    
    # Pattern 4: Long parameter lists in function definitions
    param_pattern = r'(.*\([^,)]*),\s*([^,)]+),\s*([^)]+.*)'
    match = re.match(param_pattern, line)
    if match and ('fun ' in match.group(1) or 'class ' in match.group(1)):
        prefix, param1, rest = match.groups()
        return [
            f'{prefix},',
            f'{base_indent}    {param1},',
            f'{base_indent}    {rest}'
        ]
    
    # If no pattern matches, try to break at logical points
    return break_at_logical_points(line, base_indent)

def break_at_logical_points(line, base_indent):
    """Break line at logical points like operators, commas, etc."""
    
    # Try to break at binary operators
    operators = [' && ', ' || ', ' + ', ' - ', ' * ', ' / ', ' == ', ' != ', ' <= ', ' >= ', ' < ', ' > ']
    
    for op in operators:
        if op in line:
            parts = line.split(op, 1)
            if len(parts) == 2 and len(parts[0]) < MAX_LINE_LENGTH:
                return [
                    f'{parts[0]}{op.rstrip()}',
                    f'{base_indent}    {op.lstrip()}{parts[1]}'
                ]
    
    # Try to break at commas
    if ', ' in line:
        # Find a good comma to break at
        comma_positions = [m.start() for m in re.finditer(', ', line)]
        for pos in comma_positions:
            if pos < MAX_LINE_LENGTH - 20:  # Leave some buffer
                before = line[:pos + 1]
                after = line[pos + 2:]
                return [
                    before,
                    f'{base_indent}    {after}'
                ]
    
    # If we can't break it nicely, just return the original line
    return line

def process_file(file_path):
    """Process a single Kotlin file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        fixed_content = fix_long_lines(content)
        
        if fixed_content != content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(fixed_content)
            print(f"Fixed line lengths in {file_path}")
            return True
        
        return False
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def main():
    """Main function."""
    if len(sys.argv) < 2:
        print("Usage: fix-line-lengths.py <file1> [file2] ...")
        sys.exit(1)
    
    files_changed = 0
    
    for file_path in sys.argv[1:]:
        if file_path.endswith('.kt'):
            if process_file(file_path):
                files_changed += 1
    
    if files_changed > 0:
        print(f"Fixed line lengths in {files_changed} files")
        sys.exit(1)  # Exit with 1 to indicate changes were made
    else:
        print("No line length fixes needed")
        sys.exit(0)

if __name__ == "__main__":
    main()