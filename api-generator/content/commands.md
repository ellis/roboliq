---
title: Commands & Types
layout: commands.html
lookupCollection: commands
---

This page lists the commands and types available in Roboliq protocols.
Note that:

* Types begin with an uppercase letter, whereas command and namespaces begin with a lowercase
letter.

* A namespace group related commands and types, such as the `pipetter` namespace
contains items relevant to pipetting.  Normally the namespaces will be nouns by
convention.

* As a matter of convention, all commands belong to a namespace.  Normally
commands will start with a verb.  For example, `pipetter.mix` is a command to
use the pipetter to mix well contents.

* Commands and types that begin with an underscore, such as `pipetter._mix`,
are low-level commands.  Normally you will not need to use them and can use
the other higher-level commands instead.

## Basic types

### Duration

Duration specified as a string.  Parsing is handled by [mathjs](http://mathjs.org/docs/datatypes/units.html).
Valid units are:
second (s, secs, seconds), minute (mins, minutes), hour (h, hr, hrs, hours), day (days), week (weeks), month (months), year (years), decade (decades), century (centuries), millennium (millennia)

**Example**:

* `1s` = 1 second
* `3 minutes` = 3 minutes
* `4 h` = 4 hours

### SiteOrStay

When a command is performed on a plate, sometimes the plate first needs to
be moved to the proper equipment (e.g. you might put a plate onto a shaker in
order to shake it).  By default, the plate will be moved back to its prior
location after the command is finished.  Such commands have a `destinationAfter`
property with type `SiteOrStay` which can be used to override this behavior.
Either you can specify different `Site` where the plate should be moved to after
the command, or you can specify `"stay"` to leave the plate there.

## Object types
