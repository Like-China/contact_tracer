import matplotlib.pyplot as plt


f = open("/home/like/data/contact_tracer/porto20_10000.txt")
# for jj in range(10):
x = []
y = []
for ii in range(10000):
    line = f.readline().split(" ")
    x.append(float(line[1]))
    y.append(float(line[2]))
plt.figure()
plt.scatter(x, y)
plt.show()
f.close()