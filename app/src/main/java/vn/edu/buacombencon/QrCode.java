package vn.edu.buacombencon;

import java.nio.charset.StandardCharsets;

/** Version 1, byte mode, error correction L, fixed mask 0. Payload <=17 ASCII bytes. */
public final class QrCode {
    public final boolean[][] modules=new boolean[21][21];
    private final boolean[][] function=new boolean[21][21];
    private void put(int x,int y,boolean v) { modules[y][x]=v;function[y][x]=true; }
    private static int multiply(int a,int b) {
        int out=0;for(int i=7;i>=0;i--){out=(out<<1)^((out>>>7)*0x11d);out^=((b>>>i)&1)*a;}return out;
    }
    private void finder(int cx,int cy) {
        for(int dy=-4;dy<=4;dy++)for(int dx=-4;dx<=4;dx++) {
            int x=cx+dx,y=cy+dy;
            if(x>=0&&y>=0&&x<21&&y<21) {int d=Math.max(Math.abs(dx),Math.abs(dy));put(x,y,d!=2&&d!=4);}
        }
    }
    public QrCode(String payload) {
        byte[] raw=payload.getBytes(StandardCharsets.US_ASCII);if(raw.length>17)throw new IllegalArgumentException("QR payload too long");
        int[] data=new int[26];boolean[] bits=new boolean[152];int pos=0;
        int[] prefix={0,1,0,0};for(int b:prefix)bits[pos++]=b!=0;
        for(int i=7;i>=0;i--)bits[pos++]=((raw.length>>>i)&1)!=0;
        for(byte b:raw)for(int i=7;i>=0;i--)bits[pos++]=((b>>>i)&1)!=0;
        pos=Math.min(152,pos+4);pos=(pos+7)/8*8;
        for(int i=0;i<pos;i++)if(bits[i])data[i/8]|=1<<(7-i%8);
        for(int i=pos/8;i<19;i++)data[i]=((i-pos/8)%2==0)?0xec:0x11;
        int[] divisor=new int[7];divisor[6]=1;int root=1;
        for(int i=0;i<7;i++) {
            for(int j=0;j<7;j++){divisor[j]=multiply(divisor[j],root);if(j+1<7)divisor[j]^=divisor[j+1];}
            root=multiply(root,2);
        }
        int[] rem=new int[7];for(int i=0;i<19;i++) {
            int factor=data[i]^rem[0];System.arraycopy(rem,1,rem,0,6);rem[6]=0;
            for(int j=0;j<7;j++)rem[j]^=multiply(divisor[j],factor);
        }
        System.arraycopy(rem,0,data,19,7);
        for(int i=0;i<21;i++){put(6,i,i%2==0);put(i,6,i%2==0);}
        finder(3,3);finder(17,3);finder(3,17);
        int format=0x77c4;
        for(int i=0;i<=5;i++)put(8,i,((format>>>i)&1)!=0);
        put(8,7,((format>>>6)&1)!=0);put(8,8,((format>>>7)&1)!=0);put(7,8,((format>>>8)&1)!=0);
        for(int i=9;i<15;i++)put(14-i,8,((format>>>i)&1)!=0);
        for(int i=0;i<8;i++)put(20-i,8,((format>>>i)&1)!=0);
        for(int i=8;i<15;i++)put(8,6+i,((format>>>i)&1)!=0);
        put(8,13,true);
        int bit=0;for(int right=20;right>=1;right-=2) {
            if(right==6)right=5;
            for(int vert=0;vert<21;vert++)for(int j=0;j<2;j++) {
                int x=right-j,y=((right+1)&2)==0?20-vert:vert;
                if(!function[y][x]){boolean val=bit<208&&((data[bit/8]>>>(7-bit%8))&1)!=0;bit++;modules[y][x]=val^((x+y)%2==0);}
            }
        }
    }
}
