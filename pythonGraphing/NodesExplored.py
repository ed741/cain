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
    xAxis = "nodes"
    yAxis = "length"
    resolutionf = lambda m: int(m/10)

    path = sys.argv[1]
    print("Reading data from:", path)
    rows = getData(path)
    print("rows:, "+str(rows))
    print(len(rows))

    filters = defaultdict(list)
    maxx = 0
    for row in rows:
        tag = int(row["id"])
        filters[tag].append((int(row[xAxis]), int(row[yAxis])))
        maxx = max(maxx, int(row[xAxis]))
    resolution = resolutionf(maxx)

    xArray = np.linspace(0, maxx, resolution)
    array = np.zeros((xArray.shape[0], len(filters)))
    idx = 0
    for k, v in filters.items():
        for i in range(array.shape[0]):
            array[i][idx] = min([np.inf]+[l for x,l in v if x <= xArray[i]])
        idx += 1


    mean = np.mean(array, axis=1)
    median = np.percentile(array, 50, axis=1)
    p5th = np.percentile(array, 5, axis=1)
    p95th = np.percentile(array, 95, axis=1)

    plt.ion()
    fig = plt.figure()
    ax = fig.add_subplot()
    ax.grid()

    # ax.plot(mean, label="Mean")
    ax.plot(median, label="Median")
    ax.plot(p5th, label="$5_{th}$ Percentile")
    ax.plot(p95th, label="$95_{th}$ Percentile")

    ax.legend()
    if xAxis is "nodes":
        xlabel = "Nodes Explored"
        ax.set_title(yAxis.capitalize()+"s of the Shortest Plans Found Given the Nodes Explored So Far")
    elif xAxis is "time":
        xlabel = "Search Time"
        ax.set_title(yAxis.capitalize()+"s of the Shortest Plans Found Given Search Time")
    else:
        xlabel = xAxis.capitalize()
        ax.set_title("Graph of Plan "+yAxis.capitalize()+"s Found Against "+ xlabel)
    ax.set(xlabel=xlabel, ylabel='Smallest Plan '+ yAxis.capitalize() +'s Found')

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


if __name__ == "__main__":
    main()
