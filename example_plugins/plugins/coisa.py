#! /usr/bin/python
import sys

for x in sys.stdin.readlines():
	print(sum(int(y) for y in x.split(" ")[1:]))
