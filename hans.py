import os
from concurrent.futures import ThreadPoolExecutor, as_completed

def load_replacements_from_header(header_path):
    file_specific_replacements = {}
    current_file = None
    with open(header_path, 'r', encoding='utf-8') as header:
        for line in header:
            stripped_line = line.strip()
            if stripped_line.startswith('<') and stripped_line.endswith('>'):
                current_file = os.path.abspath(stripped_line[1:-1])
                file_specific_replacements[current_file] = {}
            elif current_file and '|' in stripped_line:
                original, translation = stripped_line.split('|', 1)
                file_specific_replacements[current_file][original.strip()] = translation.strip()
    return file_specific_replacements

script_dir = os.path.dirname(os.path.abspath(__file__))
header_file_path = os.path.join(script_dir, "replacements.txt") 

file_specific_replacements = load_replacements_from_header(header_file_path)

def apply_replacements(file_path, replacements):
    if not os.path.isfile(file_path):
        print(f"警告: 文件 {file_path} 不存在，跳过处理。")
        return
    
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
        
        for old_str, new_str in replacements.items():
            content = content.replace(old_str, new_str)
        
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(content)
        
        print(f"已处理文件: {file_path}")
    except Exception as e:
        print(f"处理文件 {file_path} 时出错: {e}")

def process_files_with_specific_rules(file_specific_replacements):
    with ThreadPoolExecutor() as executor:
        futures = [
            executor.submit(apply_replacements, file_path, replacements)
            for file_path, replacements in file_specific_replacements.items()
        ]
        
        for future in as_completed(futures):
            future.result()

if __name__ == "__main__":
    process_files_with_specific_rules(file_specific_replacements)