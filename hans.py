import os
from concurrent.futures import ThreadPoolExecutor, as_completed

def load_replacements_from_header(header_path):
    file_specific_replacements = {}
    current_file = None
    print(f"\n[调试] 正在读取替换规则文件: {header_path}")
    
    try:
        with open(header_path, 'r', encoding='utf-8') as header:
            for line_number, line in enumerate(header, 1):
                stripped_line = line.strip()
                if not stripped_line or stripped_line.startswith('#'):
                    continue

                if stripped_line.startswith('<') and stripped_line.endswith('>'):
                    raw_path = stripped_line[1:-1]
                    current_file = os.path.abspath(os.path.join(os.path.dirname(header_path), raw_path))
                    print(f"[调试] 第{line_number}行解析到目标文件："
                          f"\n  原始路径：{raw_path}"
                          f"\n  绝对路径：{current_file}")
                    file_specific_replacements[current_file] = {}
                    
                elif current_file and '=' in stripped_line:
                    original, translation = stripped_line.split('=', 1)
                    original = original.strip()
                    translation = translation.strip()
                    print(f"[调试] 第{line_number}行添加规则："
                          f"\n  '{original}' => '{translation}'")
                    file_specific_replacements[current_file][original] = translation
                    
    except Exception as e:
        print(f"[错误] 读取替换规则文件失败：{str(e)}")
        raise

    print("\n[调试] 最终加载的替换规则：")
    for path, rules in file_specific_replacements.items():
        print(f"  目标文件：{path}")
        for original, translation in rules.items():
            print(f"    '{original}' -> '{translation}'")
    return file_specific_replacements

def apply_replacements(file_path, replacements):

    print(f"\n{'='*50}\n[调试] 开始处理文件：{file_path}")
    
    if not os.path.isfile(file_path):
        print(f"[错误] 文件不存在，跳过处理")
        return

    try:

        if not os.access(file_path, os.R_OK):
            print(f"[错误] 文件不可读，跳过处理")
            return
        if not os.access(file_path, os.W_OK):
            print(f"[错误] 文件不可写，跳过处理")
            return

        with open(file_path, 'r', encoding='utf-8', errors='replace') as file:
            original_content = file.read()
        new_content = original_content
        print(f"[调试] 文件长度：{len(original_content)}字符")

        for old_str, _ in replacements.items():
            if old_str not in original_content:
                print(f"[警告] 未找到需要替换的字符串: '{old_str}'")
        
        replacements_applied = 0
        for i, (old_str, new_str) in enumerate(replacements.items(), 1):
            if old_str not in original_content:
                continue
                
            new_content = new_content.replace(old_str, new_str)
            replacements_applied += 1
            print(f"[成功] 替换项 {i}/{len(replacements)}：'{old_str}' → '{new_str}'")

        if replacements_applied > 0:
            with open(file_path, 'w', encoding='utf-8') as file:
                file.write(new_content)
            print(f"[成功] 完成 {replacements_applied}/{len(replacements)} 处替换")
        else:
            print("[警告] 未执行任何有效替换")

    except UnicodeDecodeError:
        print("[错误] 文件编码不符合UTF-8格式")
    except Exception as e:
        print(f"[严重错误] {str(e)}")

def process_files_with_specific_rules(file_specific_replacements):
    with ThreadPoolExecutor() as executor:
        futures = [
            executor.submit(apply_replacements, file_path, replacements)
            for file_path, replacements in file_specific_replacements.items()
        ]
        for future in as_completed(futures):
            try:
                future.result()
            except Exception as e:
                print(f"[线程错误] {str(e)}")

if __name__ == "__main__":

    script_dir = os.path.dirname(os.path.abspath(__file__))
    header_file_path = os.path.join(script_dir, "replacements.h")
    
    print("[系统信息]")
    print(f"Python 版本：{os.sys.version}")
    print(f"工作目录：{os.getcwd()}")
    print(f"脚本目录：{script_dir}")
    print(f"规则文件路径：{header_file_path}")

    try:
        file_specific_replacements = load_replacements_from_header(header_file_path)
        process_files_with_specific_rules(file_specific_replacements)
    except Exception as e:
        print(f"[初始化失败] {str(e)}")
    finally:
        input("\n按回车键退出...")