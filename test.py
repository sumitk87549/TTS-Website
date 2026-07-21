import re, os, pathlib

print("Hello World!!!")

num_array = [1,2,4, 5, 6, 8, 10,-1]

print([x**2 for x in num_array if x%2==0 and x>0], sep='\n', end='\n')