"""Modrinth icon (512x512) and description banner (1600x500) from the original Matter Overdrive logo.

The eye of the logo's "D" and its sparkles are cut out of MatterOverdriveLogo.png (1.7.10 repo root), scaled up with
sharpened edges and set on the logo's blue gradient; the banner text is Helvetica Neue Light (macOS).
Needs Pillow:  python3 -m venv /tmp/venv && /tmp/venv/bin/pip install pillow && /tmp/venv/bin/python tools/make_modrinth_art.py
Writes docs/modrinth/icon.png and docs/modrinth/banner.png.
"""
import os
from PIL import Image, ImageFilter, ImageDraw, ImageFont
import math
logo=Image.open(os.path.expanduser('~/mo-reference/mo-1.7.10/MatterOverdriveLogo.png')).convert('RGB')
OUT='docs/modrinth'
FONT='/System/Library/Fonts/HelveticaNeue.ttc'
INK=(24,24,28)

def mask_from(box):
    reg=logo.crop(box); m=Image.new('L',reg.size); rp=reg.load(); mp=m.load()
    for y in range(reg.size[1]):
        for x in range(reg.size[0]):
            lum=sum(rp[x,y])/3
            mp[x,y]=max(0,min(255,int((110-lum)*255/80)))
    return m

def sharpen_alpha(m, scale, blur=0.45):
    big=m.resize((m.size[0]*scale,m.size[1]*scale),Image.LANCZOS).filter(ImageFilter.GaussianBlur(scale*blur))
    lut=[int(255/(1+math.exp(-(i-128)/12))) for i in range(256)]
    return big.point(lut)

def radial_bg(w,h,inner=(118,172,210),outer=(52,104,146),cy=0.45):
    # vectorised enough for these sizes
    bg=Image.new('RGB',(w,h)); p=bg.load()
    cx,cyy=w*0.5,h*cy; R=math.hypot(w,h)*0.55
    for y in range(h):
        for x in range(w):
            t=min(1,math.hypot(x-cx,(y-cyy)*(w/h if w>h else 1)*0.35)/R if w>h else math.hypot(x-cx,y-cyy)/R)
            t=t*t*(3-2*t)
            p[x,y]=tuple(int(inner[i]*(1-t)+outer[i]*t) for i in range(3))
    return bg

EYE=sharpen_alpha(mask_from((306,101,380,191)),6)
STAR=sharpen_alpha(mask_from((395,10,465,70)),5)

def paste_mask(img, mask, xy, color=INK, shadow=True):
    if shadow:
        sh=mask.filter(ImageFilter.GaussianBlur(max(2,mask.size[1]//40))).point(lambda v:int(v*0.35))
        img.paste((20,40,60),(xy[0]+4,xy[1]+6),sh)
    img.paste(color,xy,mask)

def scaled(mask, height):
    k=height/mask.size[1]; return mask.resize((max(1,int(mask.size[0]*k)),int(height)),Image.LANCZOS)

# --- icon ---
S=512
icon=radial_bg(S,S)
em=scaled(EYE,S*0.62)
paste_mask(icon,em,((S-em.size[0])//2,(S-em.size[1])//2))
for (sx,sy,sz) in [(404,96,104),(100,404,66)]:
    s=scaled(STAR,sz); paste_mask(icon,s,(sx-s.size[0]//2,sy-s.size[1]//2),shadow=False)
icon.save(f'{OUT}/icon.png')

# --- banner ---
W,H=1600,500
ban=radial_bg(W,H,inner=(122,176,214),outer=(48,98,140),cy=0.5)
d=ImageDraw.Draw(ban)
big=ImageFont.truetype(FONT,200,index=7)
small=ImageFont.truetype(FONT,104,index=7)
sub=ImageFont.truetype(FONT,34,index=10)
white=(255,255,255)
# measure OVER [eye] RIVE
def tw(f,t): b=d.textbbox((0,0),t,font=f); return b[2]-b[0], b
w1,b1=tw(big,'OVER'); w2,b2=tw(big,'RIVE')
cap=b1[3]-b1[1]
eye=scaled(EYE,cap*1.12)
gap=22
total=w1+gap+eye.size[0]+gap+w2
x0=(W-total)//2; ybase=205
d.text((x0-b1[0],ybase-b1[1]),'OVER',font=big,fill=white)
ex=x0+w1+gap
paste_mask(ban,eye,(ex,ybase+(cap-eye.size[1])//2))
d.text((ex+eye.size[0]+gap-b2[0],ybase-b2[1]),'RIVE',font=big,fill=white)
wm,bm=tw(small,'MATTER')
d.text((x0-bm[0]+4,ybase-24-(bm[3]-bm[1])-bm[1]),'MATTER',font=small,fill=white)
# subtitle, letter-spaced
text='1.21.10  ·  NEOFORGE  ·  UNOFFICIAL PORT'
spacing=6
widths=[d.textbbox((0,0),c,font=sub)[2] for c in text]
tot=sum(widths)+spacing*(len(text)-1)
x=(W-tot)//2; y=ybase+cap+44
for c,wc in zip(text,widths):
    d.text((x,y),c,font=sub,fill=(232,244,252)); x+=wc+spacing
for (sx,sy,sz) in [(1350,88,92),(150,400,60),(1440,380,46),(190,100,40)]:
    s=scaled(STAR,sz); paste_mask(ban,s,(sx-s.size[0]//2,sy-s.size[1]//2),shadow=False)
ban.save(f'{OUT}/banner.png')
