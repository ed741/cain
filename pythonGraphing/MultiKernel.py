import matplotlib.pyplot as plt
import matplotlib
import csv
import sys
from collections import defaultdict

import numpy as np

matplotlib.use('TkAgg')
matplotlib.rcParams.update({'font.size': 22})

def main():
  path = sys.argv[1]
  print("Reading data from:", path)
  rows = getData(path)
  print("rows:, "+str(rows))
  print(len(rows))

  filters = defaultdict(lambda: defaultdict(list))
  for row in rows:
    tag = int(row["kernels"])
    part = "S" if row["part"] == "0" else "I"
    filters[tag][part].append(row)

  columnsTuples = []
  for tag in filters:
    samplesS = defaultdict(int)
    samplesI = defaultdict(int)
    for row in filters[tag]["S"]:
      if int(row["cost"]) < 1000:
        samplesS[row["id"]] += int(row["cost"])
      else:
        print("Plan not found for:", str(row))
    valuesS = [v for k,v in samplesS.items()]
    for row in filters[tag]["I"]:
      if int(row["cost"]) < 1000:
        samplesI[row["id"]] += int(row["cost"])
      else:
        print("Plan not found for:", str(row))
    valuesI = [v for k,v in samplesI.items()]
    columnsTuples.append((tag, valuesI, valuesS))

  print(columnsTuples)

  columnsTuples.sort()
  columns = [[i, s] for t, i, s in columnsTuples]
  print(columns)

  plt.ion()
  fig = plt.figure()
  ax = fig.add_subplot()
  ax.grid()

  ticks = []
  tickLabels = []
  p = 1
  for t, i, s in columnsTuples:
    boxes = [i, s]
    poss = np.arange(p, p + len(boxes))
    p = p + len(boxes) + 1
    ticks.append(np.mean(poss))
    tickLabels.append(t)
    bp = ax.boxplot(boxes, positions=poss, widths=0.8)
    setBoxColors(bp)

  ax.set_xticks(ticks)
  ax.set_xticklabels(tickLabels)
  ax.set(xlabel='Kernel Count', ylabel='Smallest Plan Length Found')
  ax.set_title("Comparison of Shortest Plans founds for Kernels\nProcessed Individually and Simultaneously for 1000 available registers")
  # ax.axvline(x=first)
  plt.show(block=True)


def getData(path):
  with open(path, newline='') as csvfile:
    reader = csv.reader(csvfile, delimiter=',', quotechar='"')
    header = next(reader)
    rows = []
    for line in reader:
      d = {}
      for i in range(len(header)):
        d[header[i]] = line[i]
      rows.append(d)
    return rows


def setBoxColors(bp):
  plt.setp(bp['boxes'][0], color='blue')
  plt.setp(bp['caps'][0], color='blue')
  plt.setp(bp['caps'][1], color='blue')
  plt.setp(bp['whiskers'][0], color='blue')
  plt.setp(bp['whiskers'][1], color='blue')
  plt.setp(bp['medians'][0], color='blue')

  plt.setp(bp['boxes'][1], color='red')
  plt.setp(bp['caps'][2], color='red')
  plt.setp(bp['caps'][3], color='red')
  plt.setp(bp['whiskers'][2], color='red')
  plt.setp(bp['whiskers'][3], color='red')
  plt.setp(bp['medians'][1], color='red')

if __name__ == "__main__":
    main()
