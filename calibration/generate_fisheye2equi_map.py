import cv2
import numpy as np
import math
import os
import pandas as pd

assert float(cv2.__version__.rsplit('.', 1)[0]) >= 3, 'OpenCV version 3 or newer required.'

# Load Image
out_name = "equi"
img_file_name = "1"
fisheye_img = cv2.imread('./checkerboard_img/'+img_file_name+".jpg")
M, N = int(fisheye_img.shape[1]), int(fisheye_img.shape[0])
print("image size: "+str(M)+" x "+str(N))

# intrinsic parameters
a = np.loadtxt('./parameters/fisheye_model_intrinsic.csv', delimiter=",", dtype=np.float32 ) 
u0, v0 = a[6], a[5]


################
## remap func ##
################
def equirec2cylin(u_, v_):
    l = (math.pi/M)*(u_ - M*0.5)
    p = (math.pi/M)*(v_ - N*0.5)
    # np.savetxt('./parameters/'+out_name+"_lambda.csv",l, delimiter=',')
    # np.savetxt('./parameters/'+out_name+"_phi.csv",p, delimiter=',')
    print("Complete to compute lambda and phi")
    return l, p

def cylin2polar(l, p):
    theta = np.arccos(np.cos(p) * np.cos(l))
    alpha = np.arctan2(np.tan(p), np.sin(l))
    print("Complete to compute theta and alpha")
    # np.savetxt('./parameters/'+out_name+"_theta.csv",theta, delimiter=',')
    # np.savetxt('./parameters/'+out_name+"_alpha.csv",alpha, delimiter=',')
    return theta, alpha

def cylin2fisheye(l, p, rho):
    u = rho*np.sin(l)/np.sqrt(np.tan(p)*np.tan(p)+np.sin(l)*np.sin(l)) + u0
    v = rho*np.tan(p)/np.sqrt(np.tan(p)*np.tan(p)+np.sin(l)*np.sin(l)) + v0
    print("Complete to compute u and v")
    return u, v

def polar2fisheye(alpha, rho):
    u = rho*np.cos(alpha) + u0
    v = rho*np.sin(alpha) + v0
    np.savetxt('./parameters/'+out_name+"_u.csv",u, delimiter=',')
    np.savetxt('./parameters/'+out_name+"_v.csv",v, delimiter=',')
    print("Complete to compute u and v")
    return u.astype(np.float32), v.astype(np.float32)

def get_rho(theta):
    num = 0
    ans = np.zeros((N,M))
    if not os.path.isfile('./parameters/'+ out_name + '_rho.csv') :
        print("No rho file... Creating it")
        for row in theta:
            if(num%100==0):
                print("step "+str(num)+"/"+str(N))
            i = 0
            for col in row:
                if(col!=0.0):
                    temp = np.roots([a[4], a[3], a[2], a[1] + 1/np.tan(col), a[0]])
                else:
                    temp = np.roots([a[4], a[3], a[2], a[1] + 1/np.tan(0.0001), a[0]])
                ans[num,i] = temp[np.logical_and(temp>0, temp.imag==0.)].real[0]
                i  = i + 1
            num = num + 1
        pd.DataFrame(ans).to_csv("./parameters/" + out_name + "_rho.csv", header=False, index=False)
        print("Complete to compute rho file")
    else:
        print("rho file exists!")
        ans = pd.read_csv('./parameters/'+ out_name +'_rho.csv', delimiter=",",header=None, dtype=np.float32).values
        print("Complete to load rho file")
    return ans

###############
## main func ##
###############
if __name__ == '__main__':
    u_, v_ = np.repeat([range(0,M,1)],N,axis=0), np.reshape(np.repeat(np.array(range(0,N,1)),M),(N,M))
    l, p = equirec2cylin(u_,v_)
    theta, alpha = cylin2polar(l, p)
    rho = get_rho(theta)
    u,v = polar2fisheye(alpha, rho)

    # u = pd.read_csv("./parameters/"+out_name+"_u_param.csv", delimiter=",",header=None, dtype=np.float32).values
    # print("Complete to load u_param")
    # v = pd.read_csv("./parameters/"+out_name+"_v_param.csv", delimiter=",",header=None, dtype=np.float32).values
    # print("Complete to load v_param")

    img = cv2.remap(fisheye_img, u, v, cv2.INTER_LINEAR)
    cv2.imwrite('./output/fisheye2equi_result.jpg', img)
    cv2.imshow('fisheye2equi_result',img)
    print("Finished!")
    cv2.waitKey()
