import re

file_path = 'aznavrail-react-native/src/components/AzButton.tsx'
with open(file_path, 'r') as f:
    content = f.read()

# Add textColor to props interface
content = re.sub(r'fillColor\?: string;', r'fillColor?: string;\n  textColor?: string;', content)

# Destructure textColor
content = re.sub(r'fillColor,(\s+)shape', r'fillColor,\n  textColor,\1shape', content)

# Update size to 72
content = re.sub(r'const size = 48;', r'const size = 72;', content)

# Update borderWidth to 3
content = re.sub(r'borderWidth: isNone \? 0 : 2,', r'borderWidth: isNone ? 0 : 3,', content)

# Update dimensions for Rectangle and None
def replace_rectangle(match):
    return '''} else if (isRectangle) {
      containerStyle.width = size;
      containerStyle.height = 40;
      containerStyle.paddingHorizontal = 8;
      containerStyle.borderRadius = 0;
    } else if (isNone) {
       // Invisible rectangle
       containerStyle.width = size;
       containerStyle.height = 40;
    }'''

content = re.sub(
    r'\} else if \(isRectangle\) \{.*?\} else if \(isNone\) \{.*?\}',
    replace_rectangle,
    content,
    flags=re.DOTALL
)

# Update textStyle color
content = re.sub(r'color: color,', r'color: textColor || color,', content)

with open(file_path, 'w') as f:
    f.write(content)
