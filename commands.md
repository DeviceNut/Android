PixelNut Bluetooth String Commands

User Commands:

"*"
Causes a beacon effect to allow locating devices.

"@ <name>"
Set name of bluetooth device.

"0"
Returns the current pattern string.

"1", "2",..."13"
Changes the current preset pattern to (1-13).

"% <percent>"
Sets the current brightness percentage (0..100).

": <byte_val>"
Sets the current delay offset (-128..127).

"_<0/1>"
Switches external mode on/off.

"= <hue> <white> [<count>]"
Set hue (0..359), whiteness (0..100), and pixel count (1..max). The count is optional. The effect of changing these properties depends on whether or not external mode is set.

"! <force>"
Triggers current pattern with force (0..1000).

"?"
Returns current values, in 3 lines of numbers:
1) Constants:
	a) Number of pixels (1...).
	b) Maximum effect layers (1...).
	c) Maximum effect tracks (1...).
	d) Delay offset in msecs (-128..127).
	e) Maximum brightness (0..100).
2) External property mode settings:
	a) Mode enabled state (0/1).
	b) Color hue (0..359).
	c) Whiteness (0..100).
	d) Pixel count (1..).
3) Current effect settings:
	a) Preset pattern number.
	b) Delay in msecs (-128..127).
	c) Brightness (0..100).
