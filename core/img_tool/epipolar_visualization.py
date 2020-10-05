from matplotlib import pyplot as plt
import numpy as np

angle_from = -90
angle_to = 91
angle_step = 10

x_ = np.arange(angle_from,angle_to,angle_step)
x = np.radians(x_)
lambda_l = np.reshape(np.repeat(x,x.shape[0],axis=0),(x.shape[0],x.shape[0]))
phi_l = np.repeat([x],x.shape[0],axis=0)

# specific case: l = 0
# phi_r = np.arctan2(np.tan(phi_l)*np.cos(lambda_l),1)

# generalized
# angle = 10
for angle in range(0,91,10):
	l = np.radians(angle)
	# phi_r = np.arctan2(np.cos(lambda_l)*np.sqrt(1-np.power(np.cos(phi_l)*np.cos(l),2)),np.cos(phi_l)*np.cos(l)) # root(1-sin^2) form
	# phi_r = np.arctan2(np.cos(lambda_l)*np.sin(np.arccos(np.cos(phi_l)*np.cos(l))),np.cos(phi_l)*np.cos(l)) # cos^-1 form
	# phi_r[phi_l > 0] = -phi_r[phi_l > 0]
	phi_r = np.arctan2(np.cos(lambda_l)*np.sin(phi_l),np.cos(phi_l)*np.cos(l)) #??? idk why but it works

	plt.cla()
	plt.title("azimuth angle: "+str(angle)+"(degree)")
	plt.xlabel("\u03BB")
	plt.ylabel("\u03C6")
	plt.plot(x_,phi_r*180/np.pi)
	# plt.show()
	plt.savefig('./epipolar_line_visualization/epipolar_'+str(angle)+'.png')