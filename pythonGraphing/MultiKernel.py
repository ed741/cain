import matplotlib.pyplot as plt
import matplotlib
import csv
import sys
from collections import defaultdict

import numpy as np

matplotlib.use('TkAgg')
matplotlib.rcParams.update({'font.size': 22})
plt.rc('pgf', texsystem='pdflatex')

def main():
  path = sys.argv[1]
  print("Reading data from:", path)
  rows = getData(path)
  print("rows:, "+str(rows))
  print(len(rows))

  filters = defaultdict(list)
  for row in rows:
    tag = int(row["kernels"])
    filters[tag].append(row)

  columnsTuples = []
  for tag in filters:
    samples = defaultdict(int)
    for row in filters[tag]:
      if int(row["cost"]) < 1000:
        samples[row["id"]] += int(row["cost"])
      else:
        print("Plan not found for:", str(row))
    values = [v for k,v in samples.items()]
    columnsTuples.append((tag, values))

  print(columnsTuples)

  columnsTuples.sort()
  columns = [s for t, s in columnsTuples]
  print(columns)

  plt.ion()
  fig = plt.figure()
  ax = fig.add_subplot()
  ax.grid()

  ticks = []
  tickLabels = []
  p = 1
  for t, s in columnsTuples:
    boxes = [s]
    poss = np.arange(p, p + len(boxes))
    p = p + len(boxes) + 1
    ticks.append(np.mean(poss))
    tickLabels.append(t)
    bp = ax.boxplot(boxes, positions=poss, widths=0.8)
    setBoxColors(bp)

  ax.set_xticks(ticks)
  ax.set_xticklabels(tickLabels)

  hR, = ax.plot([1, 1], c='red')
  ax.legend((hR,), ('Simultaneous Kernels',))

  hR.set_visible(False)

  ax.set(xlabel='Kernels Compiled', ylabel='Smallest Plan Length Found')
  ax.set_title("Shortest Plans Found for Kernels Processed Simultaneously")
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
  plt.setp(bp['boxes'][0], color='red')
  plt.setp(bp['caps'][0], color='red')
  plt.setp(bp['caps'][1], color='red')
  plt.setp(bp['whiskers'][0], color='red')
  plt.setp(bp['whiskers'][1], color='red')
  plt.setp(bp['medians'][0], color='red')

if __name__ == "__main__":
    main()
