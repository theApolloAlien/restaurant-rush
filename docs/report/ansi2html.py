"""Convert ANSI-coloured terminal output (256-colour SGR) to a styled HTML <pre> block."""
import html, re, sys

BASE16 = {30:'#2e3440',31:'#ff6b6b',32:'#8fd18f',33:'#f5d76e',34:'#82aaff',35:'#d79bff',36:'#89ddff',37:'#e5e9f0',90:'#7a8394'}

def xterm(n):
    if n < 16:
        table = ['#000000','#cd0000','#00cd00','#cdcd00','#0000ee','#cd00cd','#00cdcd','#e5e5e5',
                 '#7f7f7f','#ff0000','#00ff00','#ffff00','#5c5cff','#ff00ff','#00ffff','#ffffff']
        return table[n]
    if n < 232:
        n -= 16
        steps = [0, 95, 135, 175, 215, 255]
        return '#%02x%02x%02x' % (steps[n // 36], steps[(n // 6) % 6], steps[n % 6])
    v = 8 + (n - 232) * 10
    return '#%02x%02x%02x' % (v, v, v)

SGR = re.compile(r'\x1b\[([0-9;]*)m')

def convert(text):
    out, state, pos = [], {}, 0
    def open_span():
        styles = []
        if 'fg' in state: styles.append('color:' + state['fg'])
        if state.get('bold'): styles.append('font-weight:700')
        if state.get('dim'): styles.append('opacity:.6')
        if state.get('italic'): styles.append('font-style:italic')
        return '<span style="%s">' % ';'.join(styles) if styles else '<span>'
    out.append('<span>')
    for m in SGR.finditer(text):
        out.append(html.escape(text[pos:m.start()]))
        pos = m.end()
        codes = [int(c) for c in m.group(1).split(';') if c != ''] or [0]
        i = 0
        while i < len(codes):
            c = codes[i]
            if c == 0: state = {}
            elif c == 1: state['bold'] = True
            elif c == 2: state['dim'] = True
            elif c == 3: state['italic'] = True
            elif c == 38 and i + 2 < len(codes) and codes[i+1] == 5:
                state['fg'] = xterm(codes[i+2]); i += 2
            elif c in BASE16: state['fg'] = BASE16[c]
            i += 1
        out.append('</span>' + open_span())
    out.append(html.escape(text[pos:]) + '</span>')
    return ''.join(out)

if __name__ == '__main__':
    src = open(sys.argv[1], encoding='utf-8').read()
    title = sys.argv[3] if len(sys.argv) > 3 else 'Terminal'
    page = f"""<!doctype html><meta charset=utf-8><title>{html.escape(title)}</title>
<style>body{{background:#1b1d23;margin:0;padding:24px}}
pre{{font:13px/1.35 'JetBrains Mono','SF Mono',Menlo,monospace;color:#d8dee9;background:#22252d;padding:20px 24px;border-radius:10px;width:max-content}}</style>
<pre>{convert(src)}</pre>"""
    open(sys.argv[2], 'w', encoding='utf-8').write(page)
