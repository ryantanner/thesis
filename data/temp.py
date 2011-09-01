count = 1
print count
def fact2(n):
    global count
    print "count =", count
    count+=1
    if (n == 0):
        print "n = 0"
        f2 = 1
    else:
        f2 = 0
        for i in range(0,n):
            print i, n, f2
            f2 = f2 + fact2(n-1)
    print "f2 = ", f2
    return f2

input = raw_input("Input: ")
print "i n f2"
fact = fact2(int(input))
print fact
