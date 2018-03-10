# Libraries
import matplotlib.pyplot as plt
import numpy as np;
import pandas as df
import csv
import os
from math import pi


def maw(row):
    methodAndWindow = str(row[0]).split('-')

    method = methodAndWindow[0]
    window = methodAndWindow[1]

    return method, window

def mawt(row):
    methodWindowThreshold = str(row[0]).split('-')

    method = methodWindowThreshold[0]
    window = methodWindowThreshold[1]
    threshold = methodWindowThreshold[2]

    return method, window, threshold


def prep_fig(ax, categories):
    N = len(categories)

    # What will be the angle of each axis in the plot? (we divide the plot / number of variable)
    angles = [n / float(N) * 2 * pi for n in range(N)]
    angles += angles[:1]

    plt.sca(ax)
    # Draw one axe per variable + add labels labels yet
    plt.xticks(angles[:-1], categories, color='grey', size=8)

    # Draw ylabels
    ax.set_rlabel_position(0)
    plt.yticks([0, .25, .5, .75, 1], ["0", ".25", ".5", ".75", "1"], color="grey", size=7)
    plt.ylim((0, 1))

    return N, angles


def plot_one(ax, angles, values, c='b', alpha=0.1, size=2):
    values.append(values[0])

    if all(v == 0 for v in values):
        ax.plot(0, 0, 'bo')
    else:
        # Plot data
        ax.plot(angles, values, linewidth=size, color=c, alpha=alpha, linestyle='solid')

        # Fill area
        #ax.fill(angles, values, color=c, alpha=alpha)


def plot_detector(ax, subject, detector, categories):
    N, angles = prep_fig(ax, categories)

    idx = subject[0]
    subject = subject[1]

    total = [0] * N

    inc = 1/len(subject[detector].keys())

    max_size = 12
    sz_inc = max_size / len(subject[detector].keys())
    colour = 0
    size = max_size;
    for key in subject[detector].keys():

        values = subject[detector][key]

        total = [total[i] + values[i] for i, x in enumerate(values)]

        plot_one(ax, angles, values, (0, colour, colour), 1, size=size)
        colour = colour + inc
        size = size - sz_inc

    ax.set_title(idx, loc='left')
    total = [x / len(subject[detector].keys()) for x in total]

    return total
    # plt.figtext(0, 1,
    #    r"$ \Bigg[ \begin{{matrix}}{:3.2f} & {:3.2f} \\ {:3.2f} & {:3.2f} \end{{matrix}} \Bigg] $".format(total[0],
    #                                                                                                    total[1],
    #                                                                                                    total[2],
    #                                                                                                 total[3]), size=8)


def get_iarl():
    return [80.76363636363637,
            62.63380281690141,
            95.07608695652173,
            275.03333333333336,
            44.95959595959596,
            30.565068493150687]


def convert_values(row_data):
    values = row_data[2:]
    values = [float(x) for x in values]
    frames = values[0]
    arl = values[1]

    iarl = get_iarl()
    # convert ARL to [0..1]
    values[1] = abs(arl - iarl[0]) / (frames - iarl[0])

    # convert TTD to [0..1]
    if values[2] < 0:
        values[2] = 0

    values[2] = values[2] / frames
    values = values[1:]  # trim frames
    return values


def read_maw(file):
    subj = dict()
    with open(file, 'rt') as csvfile:
        results = csv.reader(csvfile)
        for row in results:
            if row[1] in subj:
                method, window = maw(row)

                if method not in subj[row[1]]:
                    subj[row[1]][method] = dict()

                subj[row[1]][method][window] = convert_values(row)
            else:
                sdict = dict()
                method, window = maw(row)

                sdict[method] = dict()

                sdict[method][window] = convert_values(row)
                subj[row[1]] = sdict

    return subj


def read_mawt(file):
    subj = dict()
    with open(file, 'rt') as csvfile:
        results = csv.reader(csvfile)
        for row in results:
            if row[1] in subj:
                method, window, threshold = mawt(row)

                if "{}-{}".format(method, threshold) not in subj[row[1]]:
                    subj[row[1]]["{}-{}".format(method, threshold)] = dict()

                subj[row[1]]["{}-{}".format(method, threshold)][window] = convert_values(row)
            else:
                sdict = dict()
                method, window, threshold = mawt(row)

                sdict["{}-{}".format(method, threshold)] = dict()

                sdict["{}-{}".format(method, threshold)][window] = convert_values(row)
                subj[row[1]] = sdict

    return subj


def ex1():
    subj = read_maw('../../../results.csv')

    # number of variable
    categories = ['ARL', 'TTD', 'FAR', 'MDR']

    subjects = [(x, df.DataFrame(subj)[x]) for x in ['1', '2', '3', '4', '5', '6']]

    np.set_printoptions(precision=3)
    np.set_printoptions(suppress=True)

    sd = df.DataFrame(subj)
    detectors = sd['1'].keys()

    for detector in detectors:
        print(detector)

        f, axarr = plt.subplots(2, 3, subplot_kw=dict(projection='polar'))

        f.tight_layout()

        totals = np.zeros((6, len(categories)));

        totals[0, :] = plot_detector(axarr[0, 0], ('1', sd['1']), detector, categories)
        totals[1, :] = plot_detector(axarr[0, 1], ('2', sd['2']), detector, categories)
        totals[2, :] = plot_detector(axarr[0, 2], ('3', sd['3']), detector, categories)
        totals[3, :] = plot_detector(axarr[1, 0], ('4', sd['4']), detector, categories)
        totals[4, :] = plot_detector(axarr[1, 1], ('5', sd['5']), detector, categories)
        totals[5, :] = plot_detector(axarr[1, 2], ('6', sd['6']), detector, categories)

        mean = np.mean(totals, axis=0)
        meanStats = r"$\left(\begin{{matrix}}{:.4f}&{:.4f}\\{:.4f}&{:.4f}\end{{matrix}}\right)$".format(
            mean[0], mean[1], mean[2], mean[3]
        )

        print(meanStats)
        # f.suptitle(detector)

        if not os.path.exists('ex1'):
            os.makedirs('ex1')

        plt.savefig(detector + '.png', bbox_inches='tight')

    plt.show()


def ex2():
    subj = read_mawt('../../../results-cf-2.csv')

    # number of variable
    categories = ['ARL', 'TTD', 'FAR', 'MDR']

    subjects = [(x, df.DataFrame(subj)[x]) for x in ['1', '2', '3', '4', '5', '6']]

    np.set_printoptions(precision=3)
    np.set_printoptions(suppress=True)

    sd = df.DataFrame(subj)
    detectors = sd['1'].keys()

    for detector in detectors:
        print(detector)

        f, axarr = plt.subplots(2, 3, subplot_kw=dict(projection='polar'))

        f.tight_layout()

        totals = np.zeros((6, len(categories)));

        totals[0, :] = plot_detector(axarr[0, 0], ('1', sd['1']), detector, categories)
        totals[1, :] = plot_detector(axarr[0, 1], ('2', sd['2']), detector, categories)
        totals[2, :] = plot_detector(axarr[0, 2], ('3', sd['3']), detector, categories)
        totals[3, :] = plot_detector(axarr[1, 0], ('4', sd['4']), detector, categories)
        totals[4, :] = plot_detector(axarr[1, 1], ('5', sd['5']), detector, categories)
        totals[5, :] = plot_detector(axarr[1, 2], ('6', sd['6']), detector, categories)

        mean = np.mean(totals, axis=0)
        meanStats = r"$\left(\begin{{matrix}}{:.4f}&{:.4f}\\{:.4f}&{:.4f}\end{{matrix}}\right)$".format(
            mean[0], mean[1], mean[2], mean[3]
        )

        print(meanStats)

        f.suptitle(detector)

        if not os.path.exists('ex2'):
            os.makedirs('ex2')

        plt.savefig('ex2/' + detector + '.png', bbox_inches='tight')

    #plt.show()

def ex3():
    subj = read_maw('../../../results-3.csv')

    # number of variable
    categories = ['ARL', 'TTD', 'FAR', 'MDR']

    subjects = [(x, df.DataFrame(subj)[x]) for x in ['1', '2', '3', '4', '5', '6']]

    np.set_printoptions(precision=3)
    np.set_printoptions(suppress=True)

    sd = df.DataFrame(subj)
    detectors = sd['1'].keys()

    for detector in detectors:
        print(detector)

        f, axarr = plt.subplots(2, 3, subplot_kw=dict(projection='polar'))

        f.tight_layout()

        totals = np.zeros((6, len(categories)));

        totals[0, :] = plot_detector(axarr[0, 0], ('1', sd['1']), detector, categories)
        totals[1, :] = plot_detector(axarr[0, 1], ('2', sd['2']), detector, categories)
        totals[2, :] = plot_detector(axarr[0, 2], ('3', sd['3']), detector, categories)
        totals[3, :] = plot_detector(axarr[1, 0], ('4', sd['4']), detector, categories)
        totals[4, :] = plot_detector(axarr[1, 1], ('5', sd['5']), detector, categories)
        totals[5, :] = plot_detector(axarr[1, 2], ('6', sd['6']), detector, categories)

        mean = np.mean(totals, axis=0)
        meanStats = r"$\left(\begin{{matrix}}{:.4f}&{:.4f}\\{:.4f}&{:.4f}\end{{matrix}}\right)$".format(
            mean[0], mean[1], mean[2], mean[3]
        )

        print(meanStats)
        f.suptitle(detector)

        if not os.path.exists('ex3'):
            os.makedirs('ex3')

        plt.savefig('ex3/' + detector + '.png', bbox_inches='tight')

    plt.show()


if __name__ == "__main__":
    ex1()
    #ex2()
    #ex3()