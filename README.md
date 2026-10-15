# pdp11-common

The PDP-11 code that needs no machine, shared by two applications:

- [pdp11javagui](https://github.com/fjalvingh/pdp11javagui), the desktop IDE for real and
  simulated PDP-11 computers, and
- [toolsapp](https://github.com/fjalvingh/toolsapp), a web application of minicomputer tools.

What is here: addresses and memory cells, the disassembler, memory file formats (absolute
loader, ROM split-byte and the rest), the MACRO-11 assembler, the diagnostics library and the
image formats it reads, and the microcode documents with the `MicrocodeBrowser` both
applications are views over.

Both applications include this repository as a git submodule at `pdp11-common/` and build it
as a module of their own reactor. It also builds on its own:

```
./mvnw verify
```

## Rules

- **Nothing here depends on either application**, nor on Swing or AWT. The main sources are
  compiled with `--limit-modules java.base`, so a Swing import is a compile error, and
  `LayeringTest` covers the test sources and the packages of the applications
  (`to.etc.pdp11.core`, `.ui`, `.app`, `.web`).
- **A class belongs here if it needs no machine.** Anything that talks to a console, a
  transport or a simulated machine is `pdp11-core`, in pdp11javagui.
- **Octal throughout**, `java.nio.file.Path` for paths, and `byte[]`/ISO-8859-1 rather than
  default-charset text.
- **Anything with a reader and a writer is tested by round trip**, in one test that does both.

A change here is a change to both applications. Commit it here, then move the submodule pointer
in whichever of them should get it.

## Licence

GNU General Public License, version 2.
