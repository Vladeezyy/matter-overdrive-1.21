"""1.21.10 -> 26.1.2: GUI drawing became render-state extraction (run from the repo root; running it again changes nothing).

- GuiGraphicsExtractor methods: drawString -> text, drawCenteredString -> centeredText, drawWordWrap -> textWithWordWrap,
  renderItem -> item, renderFakeItem -> fakeItem, renderItemDecorations -> itemDecorations, renderOutline/submitOutline
  -> outline;
- screens and widgets: render -> extractRenderState, renderBackground -> extractBackground, renderBg(g, partial, mx, my)
  -> extractBackground(g, mx, my, partial) that first draws the screen's own background (super), renderLabels ->
  extractLabels, renderWidget -> extractWidgetRenderState, renderContents -> extractContents, renderTransparentBackground
  -> extractTransparentBackground; AbstractContainerScreen draws its tooltip itself now (renderTooltip calls go).
"""
import pathlib
import re

RENAMES = [
    (r'\.drawString\(', '.text('),
    (r'\.drawCenteredString\(', '.centeredText('),
    (r'\.drawWordWrap\(', '.textWithWordWrap('),
    (r'(\b\w+)\.renderItem\((?=[^()]*(?:\([^()]*\))?[^()]*,\s*[^,()]+,\s*[^,()]+\))', r'\1.item('),
    (r'\.renderFakeItem\(', '.fakeItem('),
    (r'\.renderItemDecorations\(', '.itemDecorations('),
    (r'\.(?:renderOutline|submitOutline)\(', '.outline('),
    (r'void render\(GuiGraphicsExtractor', 'void extractRenderState(GuiGraphicsExtractor'),
    (r'super\.render\((\w+), ', r'super.extractRenderState(\1, '),
    (r'\brenderBackground\((\w+), ', r'extractBackground(\1, '),
    (r'void renderBackground\(GuiGraphicsExtractor', 'void extractBackground(GuiGraphicsExtractor'),
    (r'\brenderTransparentBackground\(', 'extractTransparentBackground('),
    (r'void renderLabels\(GuiGraphicsExtractor', 'void extractLabels(GuiGraphicsExtractor'),
    (r'super\.renderLabels\(', 'super.extractLabels('),
    (r'void renderWidget\(GuiGraphicsExtractor', 'void extractWidgetRenderState(GuiGraphicsExtractor'),
    (r'void renderContents\(GuiGraphicsExtractor', 'void extractContents(GuiGraphicsExtractor'),
    (r'\n\s*renderTooltip\(\w+, mouseX, mouseY\);', ''),
    (r'\n\s*this\.renderTooltip\(\w+, mouseX, mouseY\);', ''),
    (r'InventoryScreen\.renderEntityInInventoryFollowsMouse\(', 'InventoryScreen.extractEntityInInventoryFollowsMouse('),
]


def bg(match):
    indent, g, partial, mx, my = match.groups()
    return (f'{indent}public void extractBackground(GuiGraphicsExtractor {g}, int {mx}, int {my}, float {partial}) {{\n'
            f'{indent}    super.extractBackground({g}, {mx}, {my}, {partial});')


changed = 0
for base in ['common/src/main/java', 'neoforge/src', 'fabric/src']:
    for path in pathlib.Path(base).rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        new = text
        for pattern, repl in RENAMES:
            new = re.sub(pattern, repl, new)
        new = re.sub(r'( *)(?:protected|public) void renderBg\(GuiGraphicsExtractor (\w+), float (\w+), int (\w+), int (\w+)\) \{',
                     bg, new)
        if new != text:
            path.write_text(new, encoding='utf-8')
            changed += 1
print(changed, 'files changed')
