import os
import sys
this_dir = os.path.dirname(__file__)
sys.path.append(this_dir)
from lowpassfilter import LowPassFilter
import time
import math
import numpy as np

class OneEuroFilter(object):
    def __init__(self, freq=15, mincutoff=1, beta=0, dcutoff=1.0):
    # def __init__(self, freq=15, mincutoff=0.15, beta=1, dcutoff=1.0): # meter scale
        """
        # f_c: decrease -> reduce jitter, increase lag
            # f_c = f_cmin + beta*X
        # freq: 
        # mincutoff: 
        # beta: 
        # dcutoff: 
        """
        if freq<=0:
            raise ValueError("freq should be >0")
        if mincutoff<=0:
            raise ValueError("mincutoff should be >0")
        if dcutoff<=0:
            raise ValueError("dcutoff should be >0")
        self.__freq = float(freq)
        self.__mincutoff = float(mincutoff)
        self.__beta = float(beta)
        self.__dcutoff = float(dcutoff)
        self.__x = LowPassFilter(self.__alpha(self.__mincutoff))
        self.__dx = LowPassFilter(self.__alpha(self.__dcutoff))
        self.__lasttime = time.time()
        
    def __alpha(self, cutoff):
        te    = 1.0 / self.__freq
        tau   = 1.0 / (2*math.pi*cutoff)
        return  1.0 / (1.0 + tau/te)

    def __call__(self, x, timestamp=None):
        if timestamp is None:
            timestamp = time.time()
        # ---- update the sampling frequency based on timestamps
        if self.__lasttime and timestamp:
            self.__freq = 1.0 / (timestamp-self.__lasttime)
        self.__lasttime = timestamp
        # ---- estimate the current variation per second
        prev_x = self.__x.lastValue()
        dx = 0.0 if prev_x is None else (x-prev_x)*self.__freq # FIXME: 0.0 or value?
        edx = self.__dx(dx, timestamp, alpha=self.__alpha(self.__dcutoff))
        # ---- use it to update the cutoff frequency
        cutoff = self.__mincutoff + self.__beta*np.fabs(edx)
        # ---- filter the given value
        return self.__x(x, timestamp, alpha=self.__alpha(cutoff))