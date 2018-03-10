from glyph import *
import matplotlib.pyplot as plt


def plot_glyph(ax, angles, values, title):
    prep_fig(ax, cats)
    plot_one(ax, angles, values)
    txt = ax.set_title(title)
    txt.set_y(1.2)


f = plt.figure()
ax = f.add_subplot(111, projection='polar')
cats = ['ARL', 'TTD', 'FAR', 'MDR']
N, angles = prep_fig(ax, cats)

plot_glyph(ax, angles,  [1, 1, 1, 1], '')
plot_glyph(ax, angles,  [0.8, 0.7, 0.1, 0.2], '')
plot_glyph(ax, angles,  [0.1, 0.1, 0.1, 0.1], '')
plt.savefig('archetype.png', bbox_inches='tight')

f2, axarr = plt.subplots(2, 3, subplot_kw=dict(projection='polar'))


f2.tight_layout()

plot_glyph(axarr[0, 0], angles, [1, 1, 0, 1], 'Never Signals')
plot_glyph(axarr[0, 1], angles, [0, 0, 1, 0], 'Always Signals')
plot_glyph(axarr[0, 2], angles, [0.1, 0.1, 0.1, 0.7], 'Conservative')
plot_glyph(axarr[1, 0], angles, [0.1, 0.1, .7, 0.1], 'Eager')
plot_glyph(axarr[1, 1], angles, [0.5, 0.2, 0.1, 0.1], 'Slow Reactions')
plot_glyph(axarr[1, 2], angles, [0, 0, 0, 0], 'Perfect')
plt.savefig('glyphs.png', bbox_inches='tight')
# plot_detector(axarr[0, 1], subjects[1], detector, categories)
# plot_detector(axarr[0, 2], subjects[2], detector, categories)
# plot_detector(axarr[0, 3], subjects[3], detector, categories)
# plot_detector(axarr[0, 4], subjects[4], detector, categories)
# plot_detector(axarr[0, 5], subjects[5], detector, categories)

# f.suptitle(detector)

# plt.savefig(detector + '.png', bbox_inches='tight')

plt.show()
