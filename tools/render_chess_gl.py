#!/usr/bin/env python3
"""Offscreen official 3D shots (Paparazzi cannot render GLSurfaceView)."""
from __future__ import annotations
import math, os, sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

os.environ.setdefault("PYOPENGL_PLATFORM", "egl")
import moderngl

REPO = Path("/workspace/gamehub")
DRAW = REPO / "app/src/main/res/drawable"
PRIV = REPO / "app/src/private/res/drawable"
OUT = Path("/workspace/gamehub-v2.3-shots")
OUT.mkdir(exist_ok=True)

VS = """
#version 330
in vec3 aPos; in vec3 aNrm; in vec2 aUv;
uniform mat4 uMVP, uModel;
uniform mat3 uNrmM;
out vec3 vN, vW; out vec2 vUv;
void main() {
  vec4 w = uModel * vec4(aPos, 1.0);
  vW = w.xyz; vN = normalize(uNrmM * aNrm); vUv = aUv;
  gl_Position = uMVP * vec4(aPos, 1.0);
}
"""
FS = """
#version 330
in vec3 vN, vW; in vec2 vUv;
uniform sampler2D uTex;
uniform vec4 uColor;
uniform vec3 uLight, uEye;
uniform float uAmb, uSpec, uUseTex, uCut, uEmit, uUnlit;
out vec4 frag;
void main() {
  vec4 t = (uUseTex > 0.5) ? texture(uTex, vUv) : vec4(1.0);
  vec4 c = t * uColor;
  if (c.a < uCut) discard;
  if (uUnlit > 0.5) { frag = vec4(c.rgb + c.rgb * uEmit, c.a); return; }
  vec3 n = normalize(vN);
  vec3 l = normalize(uLight);
  float ndl = max(dot(n, l), 0.0);
  vec3 v = normalize(uEye - vW);
  vec3 h = normalize(l + v);
  float sp = pow(max(dot(n, h), 0.0), 28.0) * uSpec;
  float lit = uAmb + (1.0 - uAmb) * ndl;
  frag = vec4(c.rgb * lit + vec3(sp) + c.rgb * uEmit, c.a);
}
"""

def norm(v):
    n = np.linalg.norm(v)
    return v if n < 1e-8 else v / n

def mat_ident():
    return np.eye(4, dtype=np.float32)

def mat_t(x,y,z):
    m = mat_ident(); m[0,3]=x; m[1,3]=y; m[2,3]=z; return m

def mat_s(x,y,z):
    m = mat_ident(); m[0,0]=x; m[1,1]=y; m[2,2]=z; return m

def mat_ry(a):
    c,s=math.cos(a),math.sin(a)
    m=mat_ident(); m[0,0]=c; m[0,2]=s; m[2,0]=-s; m[2,2]=c; return m

def mat_look(eye, target, up=np.array([0,1,0],np.float32)):
    f = norm(target-eye)
    s = norm(np.cross(f, up))
    u = np.cross(s, f)
    rot = mat_ident()
    rot[0,:3]=s; rot[1,:3]=u; rot[2,:3]=-f
    return rot @ mat_t(-eye[0], -eye[1], -eye[2])

def mat_persp(fovy, aspect, near, far):
    t = math.tan(fovy/2)
    m = np.zeros((4,4), np.float32)
    m[0,0]=1/(aspect*t); m[1,1]=1/t
    m[2,2]=(far+near)/(near-far); m[2,3]=2*far*near/(near-far)
    m[3,2]=-1
    return m

def mul(a,b): return a @ b

# ---- meshes ----
def lathe(profile, segs=28):
    n=len(profile)
    pos=[]; nrm=[]; uv=[]
    y0,y1=profile[0][1], profile[-1][1]
    yspan=max(y1-y0,1e-4)
    for i,(r,y) in enumerate(profile):
        i0=max(i-1,0); i1=min(i+1,n-1)
        dr=profile[i1][0]-profile[i0][0]; dy=profile[i1][1]-profile[i0][1]
        ln=max(math.hypot(dr,dy),1e-5)
        nr,ny = dy/ln, -dr/ln
        for s in range(segs+1):
            a=s/segs*math.tau; c,sn=math.cos(a),math.sin(a)
            pos += [r*c, y, r*sn]
            uv += [s/segs, (y-y0)/yspan]
            nx,nz=nr*c, nr*sn
            l=max(math.sqrt(nx*nx+ny*ny+nz*nz),1e-5)
            nrm += [nx/l, ny/l, nz/l]
    idx=[]
    stride=segs+1
    for i in range(n-1):
        for s in range(segs):
            a=i*stride+s; b=a+1; c=a+stride; d=c+1
            idx += [a,c,b, b,c,d]
    return np.array(pos,np.float32), np.array(nrm,np.float32), np.array(uv,np.float32), np.array(idx,np.int32)

def disc(r,y,segs=32,up=True):
    pos=[0,y,0]; nrm=[0, 1 if up else -1, 0]; uv=[0.5,0.5]
    for s in range(segs+1):
        a=s/segs*math.tau; c,sn=math.cos(a),math.sin(a)
        pos += [r*c,y,r*sn]; nrm += [0, 1 if up else -1, 0]; uv += [0.5+0.5*c, 0.5+0.5*sn]
    idx=[]
    for s in range(segs):
        if up: idx += [0,s+1,s+2]
        else: idx += [0,s+2,s+1]
    return np.array(pos,np.float32), np.array(nrm,np.float32), np.array(uv,np.float32), np.array(idx,np.int32)

def box(x0,y0,z0,x1,y1,z1):
    faces=[
        ((x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1),(0,0,1)),
        ((x1,y0,z0),(x0,y0,z0),(x0,y1,z0),(x1,y1,z0),(0,0,-1)),
        ((x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0),(-1,0,0)),
        ((x1,y0,z1),(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(1,0,0)),
        ((x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0),(0,1,0)),
        ((x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1),(0,-1,0)),
    ]
    pos=[]; nrm=[]; uv=[]; idx=[]; v=0
    for a,b,c,d,n in faces:
        for p,uu in zip((a,b,c,d),((0,0),(1,0),(1,1),(0,1))):
            pos += list(p); nrm += list(n); uv += list(uu)
        idx += [v,v+1,v+2, v,v+2,v+3]; v+=4
    return np.array(pos,np.float32), np.array(nrm,np.float32), np.array(uv,np.float32), np.array(idx,np.int32)

def merge(parts):
    pos=[]; nrm=[]; uv=[]; idx=[]; base=0
    for p,n,u,i in parts:
        pos.append(p); nrm.append(n); uv.append(u)
        idx.append(i+base); base += p.size//3
    return np.concatenate(pos), np.concatenate(nrm), np.concatenate(uv), np.concatenate(idx)

def quad(x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3, nx,ny,nz, u0=0,v0=0,u1=1,v1=1):
    pos=np.array([x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3],np.float32)
    nrm=np.array([nx,ny,nz]*4,np.float32)
    uv=np.array([u0,v0,u1,v0,u1,v1,u0,v1],np.float32)
    idx=np.array([0,1,2,0,2,3],np.int32)
    return pos,nrm,uv,idx

def pawn():
    p=[(0,0),(0.28,0),(0.30,0.03),(0.24,0.06),(0.20,0.10),(0.22,0.13),(0.16,0.16),
       (0.12,0.42),(0.14,0.50),(0.20,0.58),(0.22,0.70),(0.18,0.82),(0.10,0.88),(0,0.90)]
    return merge([lathe(p), disc(0.28,0.001,up=False)])

def rook():
    p=[(0,0),(0.32,0),(0.34,0.03),(0.26,0.07),(0.24,0.11),(0.26,0.14),(0.18,0.18),(0.15,0.62),(0.20,0.66),(0.22,0.70),(0.22,0.86)]
    parts=[lathe(p), disc(0.22,0.86,up=True), disc(0.32,0.001,up=False)]
    for i in range(4):
        a=i*math.tau/4; x,z=math.cos(a)*0.16, math.sin(a)*0.16
        parts.append(box(x-0.07,0.86,z-0.07,x+0.07,1.02,z+0.07))
    return merge(parts)

def bishop():
    p=[(0,0),(0.30,0),(0.32,0.03),(0.24,0.07),(0.22,0.12),(0.24,0.15),(0.14,0.20),(0.11,0.55),(0.16,0.68),(0.18,0.86),(0.12,1.02),(0.05,1.10),(0.07,1.16),(0,1.18)]
    return merge([lathe(p), disc(0.30,0.001,up=False)])

def sphere(r, segs=12):
    p=[(r*math.sin(i/segs*math.pi), r*(1-math.cos(i/segs*math.pi))) for i in range(segs+1)]
    return lathe(p, segs)

def queen():
    p=[(0,0),(0.33,0),(0.35,0.03),(0.26,0.07),(0.24,0.13),(0.26,0.16),(0.15,0.22),(0.13,0.62),(0.20,0.78),(0.16,0.96),(0.22,1.08),(0.14,1.16),(0,1.18)]
    parts=[lathe(p), disc(0.33,0.001,up=False)]
    for i in range(8):
        a=i*math.tau/8; x,z=math.cos(a)*0.18, math.sin(a)*0.18
        sp,sn,su,si=sphere(0.045,8)
        parts.append((sp+np.array([x,1.20,z]* (sp.size//3),np.float32).reshape(-1), sn, su, si))
        # translate sphere properly
    # fix sphere translate:
    parts=parts[:2]
    for i in range(8):
        a=i*math.tau/8; x,z=math.cos(a)*0.18, math.sin(a)*0.18
        sp,sn,su,si=sphere(0.045,8)
        sp=sp.copy(); sp[0::3]+=x; sp[1::3]+=1.20; sp[2::3]+=z
        parts.append((sp,sn,su,si))
    return merge(parts)

def king():
    p=[(0,0),(0.34,0),(0.36,0.03),(0.26,0.07),(0.24,0.13),(0.26,0.16),(0.15,0.22),(0.13,0.70),(0.20,0.86),(0.16,1.04),(0.22,1.16),(0.14,1.24),(0.10,1.28)]
    return merge([lathe(p), disc(0.34,0.001,up=False),
                  box(-0.035,1.28,-0.035,0.035,1.52,0.035),
                  box(-0.14,1.38,-0.035,0.14,1.45,0.035)])

def knight():
    base_p=[(0,0),(0.30,0),(0.32,0.03),(0.24,0.07),(0.22,0.12),(0.24,0.15),(0.18,0.18),(0.16,0.32)]
    sil=[(-0.10,0.32),(0.14,0.32),(0.16,0.48),(0.18,0.62),(0.22,0.76),(0.28,0.90),
         (0.20,1.00),(0.06,1.02),(-0.02,0.96),(-0.06,0.90),(-0.02,0.88),(0.04,0.86),
         (-0.04,0.78),(-0.10,0.62),(-0.14,0.48),(-0.10,0.32)]
    half=0.11
    pos=[]; nrm=[]; uv=[]; idx=[]; n=len(sil)
    def vert(x,y,z,nx,ny,nz,u,v):
        i=len(pos)//3; pos.extend([x,y,z]); nrm.extend([nx,ny,nz]); uv.extend([u,v]); return i
    capL=[]; capR=[]
    for z,y in sil:
        capL.append(vert(-half,y,z,-1,0,0,0,y))
        capR.append(vert(half,y,z,1,0,0,1,y))
    for i in range(1,n-1):
        idx += [capL[0],capL[i],capL[i+1], capR[0],capR[i+1],capR[i]]
    for i in range(n):
        j=(i+1)%n
        z0,y0=sil[i]; z1,y1=sil[j]
        dz,dy=z1-z0,y1-y0; ln=max(math.hypot(dz,dy),1e-5)
        ny,nz=dz/ln, -dy/ln
        a=vert(-half,y0,z0,0,ny,nz,0,0); b=vert(half,y0,z0,0,ny,nz,1,0)
        c=vert(half,y1,z1,0,ny,nz,1,1); d=vert(-half,y1,z1,0,ny,nz,0,1)
        idx += [a,b,c, a,c,d]
    head=(np.array(pos,np.float32),np.array(nrm,np.float32),np.array(uv,np.float32),np.array(idx,np.int32))
    return merge([lathe(base_p), disc(0.30,0.001,up=False), head])

MESH = None
def meshes():
    global MESH
    if MESH: return MESH
    MESH = dict(P=pawn(), N=knight(), B=bishop(), R=rook(), Q=queen(), K=king(),
                sq=quad(0,0.02,0, 1,0.02,0, 1,0.02,1, 0,0.02,1, 0,1,0),
                floor=disc(22,-0.01,48,True))
    # frame
    t,h=0.62,0.22
    MESH['frame']=merge([
        box(-t,0,-t,0,h,8+t), box(8,0,-t,8+t,h,8+t),
        box(0,0,-t,8,h,0), box(0,0,8,8,h,8+t),
        box(-0.07,0.015,0,0,h,8), box(8,0.015,0,8.07,h,8),
        box(0,0.015,-0.07,8,h,0), box(0,0.015,8,8,h,8.07),
    ])
    MESH['trim']=merge([
        box(-0.64,0.205,-0.64, 8.64,0.228,-0.50),
        box(-0.64,0.205, 8.50, 8.64,0.228, 8.64),
        box(-0.64,0.205,-0.50,-0.50,0.228, 8.50),
        box( 8.50,0.205,-0.50, 8.64,0.228, 8.50),
    ])
    MESH['base']=merge([
        lathe([(0,0),(0.30,0),(0.32,0.025),(0.28,0.05),(0.24,0.07),(0.26,0.10),(0.22,0.14),(0.20,0.16),(0.18,0.18)]),
        disc(0.18,0.18,24,True), disc(0.30,0.001,24,False),
    ])
    # card
    segs=12; width,height,curve=0.62,1.10,0.32
    pos=[]; nrm=[]; uv=[]; idx=[]
    for i in range(2):
        y=0 if i==0 else height; vv=i
        for s in range(segs+1):
            t=s/segs; a=(t-0.5)*curve; r=width/max(curve,0.15)
            x=math.sin(a)*r; z=(math.cos(a)-1)*r
            pos += [x,y,z]; nrm += [-math.sin(a),0,-math.cos(a)]; uv += [t,1-vv]
    stride=segs+1
    for s in range(segs):
        a=s; b=s+1; c=a+stride; d=c+1
        idx += [a,c,b, b,c,d]
    MESH['card']=(np.array(pos,np.float32),np.array(nrm,np.float32),np.array(uv,np.float32),np.array(idx,np.int32))
    # backdrop cylinder
    segs=48; radius=16; y0,y1=-1.5,11
    pos=[]; nrm=[]; uv=[]; idx=[]
    for i,y in enumerate((y0,y1)):
        for s in range(segs+1):
            a=s/segs*math.tau + math.pi/2
            c,sn=math.cos(a),math.sin(a)
            pos += [4+radius*c, y, 4+radius*sn]
            nrm += [-c,0,-sn]; uv += [s/segs, 1-i]
    stride=segs+1
    for s in range(segs):
        a=s; b=s+1; c=a+stride; d=c+1
        idx += [a,b,c, b,d,c]
    MESH['bg']=(np.array(pos,np.float32),np.array(nrm,np.float32),np.array(uv,np.float32),np.array(idx,np.int32))
    MESH['letter']=quad(-0.13,0,-0.13, 0.13,0,-0.13, 0.13,0,0.13, -0.13,0,0.13, 0,1,0)
    s=18.0; c=4.0; y0,y1=-2.0,12.0
    x0,x1,z0,z1=c-s,c+s,c-s,c+s
    MESH['sky']=merge([
        quad(x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,-1, 0,0,1,1),
        quad(x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,1, 0,0,1,1),
        quad(x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, -1,0,0, 0,0,1,1),
        quad(x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, 1,0,0, 0,0,1,1),
        quad(x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,-1,0, 0,0,1,1),
    ])
    return MESH

# start pos after ruy lopez: e2e4 e7e5 g1f3 b8c6 f1b5
# standard placement, then those moves
START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
# after moves:
# white pawn e4, black pawn e5, white knight f3, black knight c6, white bishop b5
FEN_SQ = {}  # filled below

def board_after_ruy():
    # map sq 0=a1 .. 63=h8
    def put():
        b=[None]*64
        order="RNBQKBNR"
        kinds="PNBRQK"
        for i,ch in enumerate(order):
            b[i]=(ch, "W")
            b[56+i]=(ch.lower() if ch!='K' else 'k' if False else ch.lower() if ch!='Q' else 'q', "B")
        # fix
        home="RNBQKBNR"
        for f,ch in enumerate(home):
            b[f]=(ch,"W"); b[56+f]=(ch,"B")
        for f in range(8):
            b[8+f]=("P","W"); b[48+f]=("P","B")
        # e2e4
        b[28]=b[12]; b[12]=None  # e4
        # e7e5
        b[36]=b[52]; b[52]=None
        # g1f3
        b[21]=b[6]; b[6]=None
        # b8c6
        b[42]=b[57]; b[57]=None
        # f1b5
        b[33]=b[5]; b[5]=None
        return b
    return put()

# political figure ids by start square (then apply same moves)
def starting_figs():
    # from ChessPolitical
    # white home rank 0, pawns rank 1; black home 7, pawns 6
    a=[None]*64
    # K e1, Q d1, R a1/h1, B c1/f1, N b1/g1, pawns a-h
    w = dict(K="trump", Q="melania", R=("vance","johnson"), B=("thune","jordan"), N=("barrasso","desantis"),
             P=("cruz","hawley","cotton","scalise","emmer","mcclain","tscott","blackburn"))
    bl= dict(K="jeffries", Q="clark", R=("schumer","aguilar"), B=("newsom","whitmer"), N=("lieu","warren"),
             P=("aoc","raskin","booker","schiff","warnock","murphy","klobuchar","duckworth"))
    def place(home, pawn, roster):
        a[4+home*8]=roster["K"]; a[3+home*8]=roster["Q"]
        a[0+home*8]=roster["R"][0]; a[7+home*8]=roster["R"][1]
        a[2+home*8]=roster["B"][0]; a[5+home*8]=roster["B"][1]
        a[1+home*8]=roster["N"][0]; a[6+home*8]=roster["N"][1]
        for f in range(8): a[f+pawn*8]=roster["P"][f]
    place(0,1,w); place(7,6,bl)
    # apply same moves
    def mv(fr,to):
        a[to]=a[fr]; a[fr]=None
    mv(12,28); mv(52,36); mv(6,21); mv(57,42); mv(5,33)
    return a

TEX_MAP = {
    "wood": ("tex_wood_lt","tex_wood_dk","tex_wood_fr","tex_gold","bg_study"),
    "neon": ("tex_neon_lt","tex_neon_dk","tex_metal","tex_metal","bg_neon"),
}

class Renderer:
    def __init__(self, w=1080, h=1080, samples=4):
        self.ctx = moderngl.create_standalone_context()
        try:
            self.fbo = self.ctx.framebuffer(
                color_attachments=[self.ctx.texture((w,h), 4, samples=samples)],
                depth_attachment=self.ctx.depth_renderbuffer((w,h), samples=samples),
            )
            self.resolve = self.ctx.framebuffer(color_attachments=[self.ctx.texture((w,h), 4)])
            self.msaa=True
        except Exception:
            self.fbo = self.ctx.framebuffer(
                color_attachments=[self.ctx.texture((w,h), 4)],
                depth_attachment=self.ctx.depth_renderbuffer((w,h)),
            )
            self.resolve=None; self.msaa=False
        self.w,self.h=w,h
        self.prog=self.ctx.program(vertex_shader=VS, fragment_shader=FS)
        self.vaos={}
        self.tex={}
        self.white=self.make_solid((255,255,255,255))
        meshes()

    def make_solid(self, rgba):
        t=self.ctx.texture((1,1),4, data=bytes(rgba))
        t.filter=(moderngl.LINEAR, moderngl.LINEAR)
        return t

    def load_tex(self, name, repeat=True, folder=None):
        if name in self.tex: return self.tex[name]
        for d in ([folder] if folder else [DRAW, PRIV]):
            if d is None: continue
            for ext in (".webp",".png",".jpg"):
                p=d/f"{name}{ext}"
                if p.exists():
                    im=Image.open(p).convert("RGBA")
                    t=self.ctx.texture(im.size, 4, im.tobytes())
                    t.build_mipmaps()
                    t.filter=(moderngl.LINEAR_MIPMAP_LINEAR, moderngl.LINEAR)
                    t.repeat_x=repeat; t.repeat_y=repeat
                    self.tex[name]=t
                    return t
        self.tex[name]=self.white
        return self.white

    def vao(self, key, mesh=None):
        if key in self.vaos: return self.vaos[key]
        p,n,u,i = mesh if mesh is not None else meshes()[key]
        vbo_p=self.ctx.buffer(p.tobytes()); vbo_n=self.ctx.buffer(n.tobytes()); vbo_u=self.ctx.buffer(u.tobytes())
        ibo=self.ctx.buffer(i.astype(np.uint32).tobytes())
        vao=self.ctx.vertex_array(self.prog, [(vbo_p,"3f","aPos"),(vbo_n,"3f","aNrm"),(vbo_u,"2f","aUv")], ibo)
        self.vaos[key]=vao
        return vao


    def _disc_mesh(self, segs=24):
        if getattr(self, "_disc", None) is not None:
            return self._disc
        pos=[]; nrm=[]; uv=[]; idx=[]
        pos += [0,0,0]; nrm += [0,1,0]; uv += [0.5,0.5]
        for i in range(segs+1):
            a=2*math.pi*i/segs
            x,z=math.cos(a), math.sin(a)
            pos += [x,0,z]; nrm += [0,1,0]; uv += [0.5+0.5*x, 0.5+0.5*z]
            if i>0:
                idx += [0, i, i+1] if i<segs else [0, i, 1]
        self._disc=(np.array(pos,np.float32),np.array(nrm,np.float32),np.array(uv,np.float32),np.array(idx,np.int32))
        return self._disc

    def draw(self, key, model, vp, tex, color=(1,1,1,1), amb=0.4, spec=0.3, use_tex=True, cut=0.02, emit=0.0, unlit=False, mesh=None):
        mvp = vp @ model
        # normal matrix: inverse transpose of upper 3x3
        m3=model[:3,:3]
        try:
            nrm=np.linalg.inv(m3).T.astype(np.float32)
        except np.linalg.LinAlgError:
            nrm=np.eye(3,dtype=np.float32)
        self.prog["uMVP"].write(mvp.T.tobytes())  # write column-major: numpy is row-major, .T
        self.prog["uModel"].write(model.T.tobytes())
        self.prog["uNrmM"].write(nrm.T.tobytes())
        self.prog["uColor"].value = tuple(color)
        self.prog["uAmb"].value=amb; self.prog["uSpec"].value=spec
        self.prog["uUseTex"].value=1.0 if use_tex else 0.0
        self.prog["uCut"].value=cut; self.prog["uEmit"].value=emit
        self.prog["uUnlit"].value=1.0 if unlit else 0.0
        tex.use(0)
        self.prog["uTex"].value=0
        self.vao(key, mesh).render()

    def render_scene(self, board="wood", pieces="staunton", political=False, likeness=False,
                      cam=None, only_sq=None):
        self.fbo.use(); self.ctx.clear(0.05,0.05,0.07,1.0)
        self.ctx.enable(moderngl.DEPTH_TEST)
        self.ctx.enable(moderngl.BLEND)
        self.ctx.blend_func = (moderngl.SRC_ALPHA, moderngl.ONE_MINUS_SRC_ALPHA)
        yaw,pitch,dist=0.42,0.72,11.5
        target=np.array([4,0.25,4],np.float32)
        fov=0.95
        if cam:
            yaw=cam.get("yaw", yaw); pitch=cam.get("pitch", pitch); dist=cam.get("dist", dist)
            target=np.array(cam.get("target", target), np.float32)
            fov=cam.get("fov", fov)
        eye=np.array([
            target[0]+math.sin(yaw)*math.cos(pitch)*dist,
            target[1]+math.sin(pitch)*dist,
            target[2]+math.cos(yaw)*math.cos(pitch)*dist,
        ],np.float32)
        view=mat_look(eye,target)
        proj=mat_persp(fov, self.w/self.h, 0.2, 80)
        vp=proj@view
        light=norm(np.array([0.45,0.82,0.35],np.float32))
        self.prog["uLight"].value=tuple(light)
        self.prog["uEye"].value=tuple(eye)

        if pieces=="political_parody":
            lt,dk,fr,tr,bg = "tex_wood_lt","tex_wood_dk","tex_wood_fr","tex_gold","bg_capitol"
        elif pieces=="political_likeness":
            lt,dk,fr,tr,bg = "tex_wood_lt","tex_wood_dk","tex_wood_fr","tex_gold","bg_oval"
        elif board=="neon":
            lt,dk,fr,tr,bg = "tex_neon_lt","tex_neon_dk","tex_metal","tex_metal","bg_neon"
        else:
            lt,dk,fr,tr,bg = "tex_wood_lt","tex_wood_dk","tex_wood_fr","tex_gold","bg_study"

        # backdrop unlit
        self.ctx.disable(moderngl.CULL_FACE)
        self.draw("sky", mat_ident(), vp, self.load_tex(bg, repeat=False), (1,1,1,1), use_tex=True, unlit=True, emit=0.12, cut=0.02)
        self.draw("bg", mat_ident(), vp, self.load_tex(bg, repeat=False), (1,1,1,1), use_tex=True, unlit=True, emit=0.08, cut=0.02)
        flcol=(0.04,0.05,0.08,1) if board=="neon" else (0.12,0.10,0.09,1)
        self.draw("floor", mat_ident(), vp, self.white, flcol, use_tex=False, amb=0.7, spec=0.05)
        self.ctx.enable(moderngl.CULL_FACE)
        self.draw("frame", mat_ident(), vp, self.load_tex(fr), (1,1,1,1), amb=0.38, spec=0.25)
        self.draw("trim", mat_ident(), vp, self.load_tex(tr), (1,1,1,1), amb=0.4, spec=0.55, emit=0.25 if board=="neon" else 0)

        # squares
        self.ctx.disable(moderngl.CULL_FACE)
        last_from, last_to, sel, checks = 5, 33, 42, None  # f1, b5, c6
        legal={27,36,38,39,19}  # d4 e5 g5 h4 d2 in 0-63: d4=27, e5=36, g5=38, h4=31? 
        # sq: file + rank*8, a1=0, d2=11, d4=27, e5=36, g5=38, h4=31, f1=5, b5=33, c6=42
        legal={27,36,38,31,11}
        caps={36}
        for sq in range(64):
            f,r=sq%8, sq//8
            light_sq=((f+r)%2)==0
            tex=self.load_tex(lt if light_sq else dk)
            tint=(1.15,1.08,0.95,1) if light_sq else (0.70,0.48,0.22,1)
            model=mat_t(f, 0.004, r)
            self.draw("sq", model, vp, tex, tint, amb=0.72, spec=0.06, use_tex=True, unlit=False)
            ov=None
            if sq==sel: ov=(0.91,0.82,0.31,0.40)
            elif sq in (last_from,last_to): ov=(0.94,0.85,0.47,0.32)
            elif sq in caps: ov=(0.85,0.2,0.15,0.28)
            elif sq in legal: ov=(0.16,0.10,0.04,0.22)
            if ov:
                self.draw("sq", mat_t(f,0.001,r), vp, self.white, ov, use_tex=False, unlit=True, amb=1)

        # coords A-H / 1-8
        letter=self.load_tex("tex_letter", repeat=False)
        for i in range(8):
            # custom UV quad per letter
            col,row=i%4, i//4
            u0,v0=col/4, 1-(row+1)/4; u1,v1= (col+1)/4, 1-row/4
            key=f"L{i}"
            mesh=quad(-0.13,0,-0.13, 0.13,0,-0.13, 0.13,0,0.13, -0.13,0,0.13, 0,1,0, u0,v0,u1,v1)
            self.draw(key, mat_t(i+0.5, 0.226, -0.30), vp, letter, (0.95,0.88,0.70,0.95), use_tex=True, unlit=True, cut=0.15, emit=0.05, mesh=mesh)
            col,row=(i+8)%4, (i+8)//4
            u0,v0=col/4, 1-(row+1)/4; u1,v1=(col+1)/4, 1-row/4
            key=f"N{i}"
            mesh=quad(-0.13,0,-0.13, 0.13,0,-0.13, 0.13,0,0.13, -0.13,0,0.13, 0,1,0, u0,v0,u1,v1)
            self.draw(key, mat_t(-0.30, 0.226, i+0.5), vp, letter, (0.95,0.88,0.70,0.95), use_tex=True, unlit=True, cut=0.15, emit=0.05, mesh=mesh)

        b=board_after_ruy()
        figs=starting_figs()
        style = "pl_" if likeness else "pp_"
        colors = {
            "staunton": ((0.965,0.941,0.878),(0.165,0.165,0.180)),
            "neon": ((0.016,0.063,0.094),(0.094,0.016,0.078)),
        }
        wc,bc = colors["neon" if board=="neon" and pieces=="neon" else "staunton"]
        emit_c = 0.18 if pieces=="neon" else 0.0
        self.ctx.disable(moderngl.CULL_FACE)
        for sq,pc in enumerate(b):
            if not pc: continue
            if only_sq is not None and sq not in only_sq: continue
            kind, side = pc
            kind=kind.upper()
            f,r=sq%8, sq//8
            world=(f+0.5, 0.0, r+0.5)
            if political:
                self.draw("base", mat_t(*world), vp, self.load_tex("tex_wood_lt"), (0.72,0.52,0.28,1), amb=0.4, spec=0.2)
                ring=(0.75,0.16,0.18,1) if side=="W" else (0.16,0.32,0.72,1)
                # ring as scaled disc - reuse floor? use base disc via scale
                # skip extra ring mesh; tint is enough
                fig=figs[sq]
                folder = PRIV if likeness else DRAW
                tex=self.load_tex(style+fig, repeat=False, folder=folder) if fig else self.white
                eye_xz=np.array([eye[0]-world[0], eye[2]-world[2]])
                yaw=math.atan2(eye_xz[0], eye_xz[1]) + math.pi
                hmap={"K":1.36,"Q":1.26,"R":1.16,"B":1.24,"N":1.18,"P":1.02}
                wsc=0.66/0.62
                sc=hmap[kind]/1.10
                # party ring on the shared 3D base
                ringm=mat_t(world[0], world[1]+0.055, world[2]) @ mat_s(0.30, 0.02, 0.30)
                self.draw("ring", ringm, vp, self.white, ring, amb=0.5, spec=0.15, use_tex=False, emit=0.05, mesh=self._disc_mesh())
                model=mat_t(world[0], world[1]+0.18, world[2]) @ mat_ry(yaw) @ mat_s(wsc,sc,1)
                self.draw("card", model, vp, tex, (1,1,1,1), amb=0.72, spec=0.06, cut=0.10)
            else:
                model=mat_t(*world)
                if side=="B":
                    model=mat_t(*world) @ mat_ry(math.pi)
                col = wc if side=="W" else bc
                self.draw(kind, model, vp, self.white, (*col,1), amb=0.38 if pieces!="neon" else 0.28,
                          spec=0.45 if pieces=="neon" else 0.32, use_tex=False, emit=emit_c)

        # read pixels
        if self.msaa:
            self.ctx.copy_framebuffer(self.resolve, self.fbo)
            tex=self.resolve.color_attachments[0]
        else:
            tex=self.fbo.color_attachments[0]
        data=tex.read()
        im=Image.frombytes("RGBA", (self.w,self.h), data).transpose(Image.FLIP_TOP_BOTTOM)
        return im

def chrome(board_im, title_right="3D"):
    """Compose a Pixel-5-ish play screen around the 3D board."""
    W,H=1080,2340
    bg=Image.new("RGB",(W,H),(22,22,28))
    d=ImageDraw.Draw(bg)
    try:
        font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 36)
        font_s=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 30)
        font_t=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 28)
    except Exception:
        font=font_s=font_t=ImageFont.load_default()
    def pill(x,y,w,h,text, f=font_s):
        d.rounded_rectangle([x,y,x+w,y+h], 22, fill=(255,255,255,30))
        # no alpha on RGB; simulate
        d.rounded_rectangle([x,y,x+w,y+h], 22, fill=(48,48,56))
        bbox=d.textbbox((0,0),text,font=f)
        tw,th=bbox[2]-bbox[0], bbox[3]-bbox[1]
        d.text((x+(w-tw)/2, y+(h-th)/2-2), text, font=f, fill=(255,255,255))
    pad=36
    pill(pad, 40, 200, 72, "‹  Menu")
    # title
    bbox=d.textbbox((0,0),"CHESS",font=font_t)
    tw=bbox[2]-bbox[0]
    d.text(((W-tw)/2, 56), "CHESS", font=font_t, fill=(200,200,210))
    pill(W-pad-280, 40, 100, 72, title_right, font)
    pill(W-pad-168, 40, 168, 72, "Reset cam")
    # board
    side=1080
    board=board_im.resize((side,side), Image.Resampling.LANCZOS)
    bx=(W-side)//2; by=140
    bg.paste(board, (bx,by))
    d.text((pad, by+side+20), "White to move", font=font_s, fill=(230,230,235))
    d.text((pad, by+side+70), "1. e4 e5  2. Nf3 Nc6  3. Bb5", font=font_s, fill=(180,180,190))
    pill(pad, H-140, 180, 72, "Resign")
    pill(pad+200, H-140, 160, 72, "Draw")
    pill(pad+380, H-140, 180, 72, "Themes")
    pill(W-pad-140, H-140, 140, 72, "Hub")
    return bg.resize((461,1000), Image.Resampling.LANCZOS)

def contact_sheet(tiles, labels, cols=6, cell=320):
    rows = (len(tiles)+cols-1)//cols
    pad, label_h = 16, 36
    W = cols*(cell+pad)+pad
    H = rows*(cell+label_h+pad)+pad
    sheet=Image.new("RGB",(W,H),(22,22,28))
    d=ImageDraw.Draw(sheet)
    try:
        font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
    except Exception:
        font=ImageFont.load_default()
    for i,(im,lab) in enumerate(zip(tiles,labels)):
        # letterbox into cell
        im=im.convert("RGB")
        sc=min(cell/im.width, cell/im.height)
        nw,nh=int(im.width*sc), int(im.height*sc)
        im=im.resize((nw,nh), Image.Resampling.LANCZOS)
        c,r=i%cols, i//cols
        x=pad+c*(cell+pad)+(cell-nw)//2
        y=pad+r*(cell+label_h+pad)+(cell-nh)//2
        sheet.paste(im,(x,y))
        bbox=d.textbbox((0,0),lab,font=font)
        tw=bbox[2]-bbox[0]
        d.text((pad+c*(cell+pad)+(cell-tw)/2, pad+r*(cell+label_h+pad)+cell+4), lab, font=font, fill=(230,230,235))
    return sheet

def main():
    r=Renderer(1080,1080, samples=4)
    # full 3D likeness board in the Oval Office
    im=r.render_scene(board="wood", pieces="political_likeness", political=True, likeness=True)
    dest=OUT/"chess_3d_likeness.png"
    chrome(im).save(dest)
    im.save(OUT/"chess_3d_likeness_raw.png")
    print("wrote", dest, dest.stat().st_size, flush=True)

    # close-ups of each side's first two ranks (K/Q/R/B/N + pawns readable)
    # White lives on ranks 1-2 (z ~ 0.5-1.5); camera from the south (low z).
    cam_w=dict(yaw=math.pi, pitch=0.42, dist=6.4, target=(4.0, 0.45, 1.15), fov=0.80)
    imw=r.render_scene(board="wood", pieces="political_likeness", political=True, likeness=True, cam=cam_w)
    dest=OUT/"chess_3d_likeness_gop_close.png"
    chrome(imw, title_right="3D").save(dest)
    imw.save(OUT/"chess_3d_likeness_gop_close_raw.png")
    print("wrote", dest, dest.stat().st_size, flush=True)

    # Black lives on ranks 7-8 (z ~ 6.5-7.5); camera from the north (high z).
    cam_b=dict(yaw=0.0, pitch=0.42, dist=6.4, target=(4.0, 0.45, 6.85), fov=0.80)
    imb=r.render_scene(board="wood", pieces="political_likeness", political=True, likeness=True, cam=cam_b)
    dest=OUT/"chess_3d_likeness_dem_close.png"
    chrome(imb, title_right="3D").save(dest)
    imb.save(OUT/"chess_3d_likeness_dem_close_raw.png")
    print("wrote", dest, dest.stat().st_size, flush=True)

    # Per-piece-type contact: one king, queen, rook, bishop, knight, pawn per side.
    # squares: W a1=R vance? starting: a1 rook, b1 knight, c1 bishop, d1 queen, e1 king, a2 pawn
    # Check starting_figs and board.
    picks = [
        # (sq, label) — filled after we know starting_figs; hardcoded common start:
        (4, "King  GOP  Trump"),      # e1
        (3, "Queen  GOP  Melania"),   # d1
        (0, "Rook  GOP  Vance"),      # a1
        (2, "Bishop  GOP  Thune"),    # c1
        (1, "Knight  GOP  Barrasso"), # b1
        (8, "Pawn  GOP  Cruz"),       # a2
        (60, "King  DEM  Jeffries"),  # e8
        (59, "Queen  DEM  Clark"),    # d8
        (56, "Rook  DEM  Schumer"),   # a8
        (58, "Bishop  DEM  Newsom"),  # c8
        (42, "Knight  DEM  Lieu"),    # c6 (moved from b8 in Ruy)
        (48, "Pawn  DEM  AOC"),       # a7
    ]
    cam_one=dict(yaw=0.15, pitch=0.28, dist=3.15, target=(0.5, 0.55, 0.5), fov=0.72)
    tiles=[]; labels=[]
    # render each selected piece by temporarily isolating it: place that
    # square's piece onto e1-like world via only_sq, but camera is aimed at
    # that square's world position.
    for sq, lab in picks:
        f, rk = sq%8, sq//8
        cam=dict(yaw=0.18, pitch=0.32, dist=3.4, target=(f+0.5, 0.55, rk+0.5), fov=0.70)
        tile=r.render_scene(board="wood", pieces="political_likeness", political=True, likeness=True,
                            cam=cam, only_sq={sq})
        tiles.append(tile); labels.append(lab)
    sheet=contact_sheet(tiles, labels, cols=6, cell=420)
    dest=OUT/"chess_3d_likeness_contact.png"
    sheet.save(dest)
    print("wrote", dest, dest.stat().st_size, sheet.size, flush=True)

if __name__=="__main__":
    main()
