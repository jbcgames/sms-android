import re

path = '/roms/ports/gamelist.xml'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if 'Super Mario Sunshine.sh' not in content:
    entry = '\t<game>\n\t\t<path>./Super Mario Sunshine.sh</path>\n\t\t<name>Super Mario Sunshine</name>\n\t\t<desc>Super Mario Sunshine native port running on RK3326 with Mali-G31 GLES3.</desc>\n\t</game>\n</gameList>'
    content = content.replace('</gameList>', entry)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("gamelist.xml updated successfully")
else:
    print("Already exists in gamelist.xml")
