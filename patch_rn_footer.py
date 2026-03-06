with open('./aznavrail-react-native/src/AzNavRail.tsx', 'r') as f:
    content = f.read()

replacement = """  const renderFooter = () => {
      const footerColor = activeColor || '#6200ee';

      const handleUndock = () => {
        if (enableRailDragging) {
            setIsFloating(true);
            setIsExpanded(false);
            logInteraction('Footer undock clicked');
        }
      };

      const handleAbout = () => {
        Linking.openURL('https://github.com/HereLiesAz/AzNavRail').catch(e => console.error("Could not open About", e));
      };

      const handleFeedback = () => {
         Linking.openURL('mailto:hereliesaz@gmail.com?subject=Feedback for AzNavRail').catch(e => console.error("Could not open Mail", e));
      };

      const handleCredit = () => {
         Linking.openURL('https://instagram.com/HereLiesAz').catch(e => console.error("Could not open Credit", e));
      };

      const handleSecLocTrigger = () => {
          setSecLocClicks(prev => prev + 1);
          if (secLocClicks >= 9) {
              setSecLocVisible(true);
              setSecLocClicks(0);
          }
      };

      return (
        <View style={[styles.footer, { alignItems: 'center' }]}>
             <View style={styles.divider} />
             {enableRailDragging && (
                 <TouchableOpacity onPress={handleUndock} style={{ paddingVertical: 8 }}><Text style={{ fontSize: 10, color: footerColor }}>Undock</Text></TouchableOpacity>
             )}
             <TouchableOpacity onPress={handleAbout} style={{ paddingVertical: 4 }}><Text style={{ fontSize: 10, color: footerColor }}>About</Text></TouchableOpacity>
             <TouchableOpacity onPress={handleFeedback} style={{ paddingVertical: 4 }}><Text style={{ fontSize: 10, color: footerColor }}>Feedback</Text></TouchableOpacity>
             <TouchableOpacity onPress={handleCredit} onLongPress={handleSecLocTrigger} delayLongPress={500} style={{ paddingVertical: 4 }}><Text style={{ fontSize: 10, color: footerColor }}>@HereLiesAz</Text></TouchableOpacity>
        </View>
      );
  };"""

import re
content = re.sub(r'  const renderFooter = \(\) => \{.*?        </View>\n      \);\n  \};', replacement, content, flags=re.DOTALL)

with open('./aznavrail-react-native/src/AzNavRail.tsx', 'w') as f:
    f.write(content)
