import os
from concurrent.futures import ThreadPoolExecutor, as_completed

def load_replacements_from_header(header_path, script_dir):
    file_specific_replacements = {}
    current_file = None
    with open(header_path, 'r', encoding='utf-8') as header:
        for line in header:
            stripped_line = line.strip()
            if stripped_line.startswith('<') and stripped_line.endswith('>'):
                # 使用script_dir作为基础路径来解析相对路径
                relative_path = stripped_line[1:-1].strip()
                current_file = os.path.abspath(os.path.join(script_dir, relative_path))
                file_specific_replacements[current_file] = {}
            elif current_file and '|' in stripped_line:
                original, translation = stripped_line.split('|', 1)
                file_specific_replacements[current_file][original.strip()] = translation.strip()
    return file_specific_replacements

script_dir = os.path.dirname(os.path.abspath(__file__))
header_file_path = os.path.join(script_dir, "replacements.h") 

file_specific_replacements = load_replacements_from_header(header_file_path, script_dir)

def apply_replacements(file_path, replacements):
    print(f"尝试处理文件: {file_path}")  # 调试输出
    if not os.path.isfile(file_path):
        print(f"警告: 文件 {file_path} 不存在，跳过处理。")
        return
    
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
        
        replaced = False
        for old_str, new_str in replacements.items():
            if old_str in content:
                print(f"找到替换项：'{old_str}' -> '{new_str}'")  # 调试输出
                content = content.replace(old_str, new_str)
                replaced = True
            else:
                print(f"未找到 '{old_str}' 在文件 {file_path} 中")  # 调试输出
        
        if replaced:
            with open(file_path, 'w', encoding='utf-8') as file:
                file.write(content)
            print(f"已处理文件: {file_path}")
        else:
            print(f"未做任何替换: {file_path}")
    except Exception as e:
        print(f"处理文件 {file_path} 时出错: {e}")

def process_files_with_specific_rules(file_specific_replacements):
    # 打印所有待处理文件及其路径是否存在
    for file_path in file_specific_replacements:
        print(f"检查文件路径: {file_path} -> 存在: {os.path.exists(file_path)}")
    
    with ThreadPoolExecutor() as executor:
        futures = [
            executor.submit(apply_replacements, file_path, replacements)
            for file_path, replacements in file_specific_replacements.items()
        ]
        
        for future in as_completed(futures):
            future.result()

if __name__ == "__main__":
    process_files_with_specific_rules(file_specific_replacements)