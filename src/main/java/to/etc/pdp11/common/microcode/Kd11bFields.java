package to.etc.pdp11.common.microcode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The PDP-11/05's microword: 19 named fields over 40 bits, and what their values mean.
 *
 * <p>The KD11-B holds 256 microwords of 40 bits in ten 256x4 PROMs, ten four-bit slices per
 * microword. Every bit is a control line and every one of them is named on the M7261 schematics,
 * so unlike the 11/44 - whose table came out of the Pascal - this one is built from the signal
 * names themselves. That is worth more than it sounds: <b>the polarity comes with the name</b>.
 * A {@code -L} line is asserted low, so its resting value is 1, and that is where
 * {@link MicrocodeField#defaultValue()} comes from for every single-bit field here rather than
 * from counting which value is commonest.</p>
 *
 * <h2>Bit numbers are the schematic's</h2>
 *
 * <p>Bit 39 is the leftmost column of the printed listing and bit 0 the rightmost, so
 * {@code SCHBIT = 39 - LISTCOL}. Those are the numbers on the drawings and on the KM11's
 * connector, which is the point: a microword read here can be compared against the lights.</p>
 *
 * <h2>One field is not what the listing's headings suggest</h2>
 *
 * <p><b>{@code SPA}</b>, the scratchpad register address, is printed as four non-adjacent and
 * out-of-order single-bit columns - {@code SP0} at bit 18, {@code SP1} at 22, {@code SP2} at 12,
 * {@code SP3} at 21. Assembled it reads as register numbers, R7 in 28 microwords and R6 in 22;
 * treated as four flags it reads as nothing at all.</p>
 *
 * <p>{@code BUT} and {@code BRG} are <i>not</i> scrambled, though this table used to say they
 * were. The schematic puts {@code BUT-1, BUT-0, BUT-2, BUT-3} on bits 3..0, and that was taken to
 * mean the printed nibble had to be put back into signal order; {@code BRG} was likewise read
 * with {@code BMODE-1} on top. Both read as printed. The microprogram flow listing in
 * EK-KD11B-MM-001 chapter 2 decides {@code BUT}: of the 13 steps it gives a branch test for, the
 * printed nibble matches all 13 and the unscrambled one 4 - {@code F-5} is the one
 * {@code IR-DECODE}, not {@code RST-1}. {@code BRG} comes from the 74194 wiring (section 4.3.6):
 * bit 5 drives S0 and bit 4 S1, so printed {@code 10} shifts right and {@code 01} left. All
 * sixteen microtests are defined, so the wrong order gave a <i>plausible</i> wrong name for every
 * microword and nothing complained; that is why the test holds it against the flow listing.</p>
 *
 * <p>The listing's {@code SM0}/{@code SM1} and {@code BTP}/{@code BBT} headings are likewise
 * pairs rather than flags, and are here as {@code SPAMUX} and {@code BLEG}. That leaves the
 * fields as they mean things rather than as they were printed, which is the whole job of a field
 * table.</p>
 *
 * <h2>The ALU is two fields, and its operation is what they say together</h2>
 *
 * <p>The listing prints the ALU control as one five-bit column, and the operation table in
 * EK-KD11B-MM-001 is keyed by all five bits. They are two things: the 74181's function select
 * {@code S3..S0} in bits 31..28, and its {@code MODE} in bit 27, high for a logic function and
 * low for an arithmetic one. Here they are the fields {@code ALU} and {@code MODE}, and since a
 * function select means nothing without the mode, the {@code ALU} field's meaning is the
 * operation the two name together - see {@link #aluOperation}.</p>
 *
 * <h2>Where AUX makes the ALU fields a don't-care</h2>
 *
 * <p>{@code AUX-CONTROL-L} selects whether {@code ALU S0..S3}, {@code ALU MODE} and {@code CIN}
 * come from this microword or are decoded from instruction register bits 15..12 or 10..6 - the
 * opcode fields of the double- and single-operand formats. That is how one microword executes
 * {@code MOV}, {@code CMP}, {@code BIT}, {@code BIC}, {@code BIS}, {@code ADD} and {@code SUB}
 * alike, and it means that <b>in a microword that asserts {@code AUX} the printed {@code ALU}
 * and {@code MODE} fields are not necessarily what the machine does</b>. See {@link #takesAluControlFromIr}.</p>
 *
 * <p>Everything here is from {@code microcode/kd11b/kd11b-fields.tsv} and
 * {@code kd11b-fieldvalues.tsv}, whose {@code kd11b-README.md} carries the transcription and how
 * far each claim is checked. The one thing that is <i>not</i> checked is any of it against a
 * running machine.</p>
 */
public final class Kd11bFields {
	/** How wide the microword is. */
	public static final int WORD_BITS = 40;

	/** How many of the 256 control store locations the listing prints. */
	public static final int LISTED_MICROWORDS = 214;

	/** How wide one control store PROM is, and so how many bits a slice of the word holds. */
	public static final int PROM_SLICE_BITS = 4;

	private Kd11bFields() {
	}

	// -----------------------------------------------------------------------------------------
	// The field names, so a lookup does not have to spell out the signal in brackets
	// -----------------------------------------------------------------------------------------

	public static final String NXT = "NXT (MPC-7..0-L, complemented)";

	public static final String ALU = "ALU (ALU-S3..S0-L)";

	public static final String MODE = "MODE (ALU-MODE-H)";

	public static final String CRI = "CRI (CIN-H)";

	public static final String FSH = "FSH (F-SHIFT-L)";

	public static final String AUX = "AUX (AUX-CONTROL-L)";

	public static final String PSW = "PSW (LOAD-PSW-L)";

	public static final String SPA = "SPA (ROM-SPA-3..0-H)";

	public static final String DIP = "DIP (ENAB-IN-PAUSE-L)";

	public static final String SPAMUX = "SPAMUX (SPA-MUX-1..0-H)";

	public static final String BLEG = "BLEG (BTOP-H, BBOT-H)";

	public static final String BAR = "BAR (BA-CLOCK-L)";

	public static final String SPF = "SPF (SP-WRITE-L)";

	public static final String CKO = "CKO (CKOFF-L)";

	public static final String ABT = "ABT (ALLOW-BYTE-L)";

	public static final String TNS = "TNS (DATO-L, DATI-L)";

	public static final String ALG = "ALG (RALEG-1..0-L)";

	public static final String BRG = "BRG (BMODE-0,1-H)";

	public static final String BUT = "BUT (BUT-1,0,2,3-L)";

	// -----------------------------------------------------------------------------------------
	// The table
	// -----------------------------------------------------------------------------------------

	/**
	 * Name a field's values, from 0 upwards, with {@code null} for a value the schematics do not
	 * name and {@code ""} for one whose name is nothing at all - "load PSW" against "do not load
	 * PSW". Both show as the number alone; the difference is whether anybody knows what it is.
	 */
	private static Map<Integer, String> values(String... byValue) {
		Map<Integer, String> map = new LinkedHashMap<>();
		for(int i = 0; i < byValue.length; i++) {
			if(byValue[i] != null)
				map.put(i, byValue[i]);
		}
		return Map.copyOf(map);
	}

	/**
	 * The ALU operations by the five bits the listing prints, {@code S3..S0} then {@code MODE},
	 * from the operation table in EK-KD11B-MM-001. All twelve codes that occur are named,
	 * covering 214 of 214 microwords. ROL and ROR are in the table but in no microword, because
	 * the rotates are not ALU functions - the ALU passes a leg through and FSH does the shifting.
	 * A XOR B is absent for a different reason: the 11/05 has no XOR instruction, which arrived
	 * with the 11/45. AL - the A leg straight through - is the resting operation and 105
	 * microwords use it.
	 */
	private static final Map<Integer, String> ALU_OPERATIONS = values(
		"AA", "AL", null, "AB", null, "A · ~B", null, "ZERO",
		null, "A + B", null, "BL", "A PLUS B", "A XOR B", null, null,
		null, null, "A MINUS B MINUS 1", null, null, "~B", null, null,
		"ASL", "MINUS ONE", "ROL", null, "ASR", null, "A MINUS 1", "~A");

	/**
	 * The KD11-B's microword, in the order the listing prints the columns.
	 *
	 * <p>All 40 bits are in exactly one field: this machine has no spare bit, which
	 * {@link MicrocodeArchitecture#unusedBits} is asserted on.</p>
	 */
	public static final MicrocodeArchitecture ARCHITECTURE;

	static {
		List<MicrocodeField> l = new ArrayList<>();

		//-- MPC-7..MPC-0, and stored active low: the next microaddress is NXT XOR 0377. Taken as
		//-- printed, 83.2% of the 214 next-addresses land on a listed location, which is chance
		//-- level because 214 of 256 are listed; complemented, 213 of 214 do. The loader does the
		//-- complementing, so the value shown here is the bits as burned.
		l.add(MicrocodeField.of(1, NXT, -1, 32, 8, Map.of()));

		//-- The 74181's function select and its mode, printed as one five-bit column. The select
		//-- has no value names of its own: what it means depends on the mode, and the field shows
		//-- the operation the two make - see aluOperation(). The mode is a selector rather than
		//-- a line that is asserted or not, so its resting value is the one AL has, not the one
		//-- its polarity would give.
		l.add(MicrocodeField.of(2, ALU, 0, 28, 4, Map.of()));
		l.add(MicrocodeField.of(3, MODE, 1, 27, 1, values("ARITHMETIC", "LOGIC")));

		l.add(MicrocodeField.of(4, CRI, 0, 26, 1, values("", "CIN")));
		l.add(MicrocodeField.of(5, FSH, 1, 25, 1, values("SHIFT", "")));
		l.add(MicrocodeField.of(6, AUX, 1, 24, 1, values("ALU CONTROL FROM IR", "")));
		l.add(MicrocodeField.of(7, PSW, 1, 23, 1, values("LOAD PSW", "")));

		//-- Trap 1: four scattered, out-of-order single-bit columns, assembled SP3 SP2 SP1 SP0.
		l.add(new MicrocodeField(8, SPA, 0, List.of(21, 12, 22, 18), values(
			"R0", "R1", "R2", "R3", "R4", "R5", "R6(SP)", "R7(PC)",
			"R10", "R11", "R12", "R13", "R14", "R15", "R16", "R17")));

		l.add(MicrocodeField.of(9, DIP, 1, 20, 1, values("ENAB IN PAUSE", "")));

		//-- Where the scratchpad address comes from. This is the independent check on SPA: all 96
		//-- microwords with a non-zero ROM scratchpad address have this set to ROM, and none of
		//-- the other 118 do. IRS and IRD cannot be told apart by the data - they are the two
		//-- values that swap when the mux bits do - so that pair is inferred, not established.
		l.add(new MicrocodeField(10, SPAMUX, 3, List.of(17, 19), values("BA", "IRD", "IRS", "ROM")));

		//-- What goes onto the B leg, the ALU's B input (EK-KD11B-MM-001 4.3.6, figure 4-8). BBOT
		//-- gates the B register's low byte, and when low puts the constant there instead and
		//-- disables the high byte's mux; BTOP is that mux's select, so it counts only with BBOT
		//-- high. The value is BTP BBT:
		//--   0  BTP=0 BBT=0  +1    the constant: bit 0 is 1, unless CONB INH +1 L makes it 0
		//--   1  BTP=0 BBT=1  SEX   the low byte, and bit 7 copied into bits 15..8
		//--   2  BTP=1 BBT=0  +1    the same as 0, since BTOP does nothing with BBOT low. The
		//--                         microcode's own +1, in 26 microwords; 0 is in none
		//--   3  BTP=1 BBT=1  BREG  the B register unmodified
		l.add(new MicrocodeField(11, BLEG, 3, List.of(14, 16), values("+1", "SEX", "+1", "BREG")));

		l.add(MicrocodeField.of(12, BAR, 1, 15, 1, values("BA CLOCK", "")));
		l.add(MicrocodeField.of(13, SPF, 1, 13, 1, values("WRITE", "READ")));

		//-- CKOFF-L, the processor clock stop. It sits among the MSYN/SSYN bus signals, but the
		//-- listing does not settle exactly when it is asserted: of the 36 microwords with CKO=0,
		//-- 22 start a bus cycle and 14 do not, and 17 microwords start a DATI without it. The
		//-- names below are the schematic's; note that they read backwards against the data, so
		//-- the line more likely enables a clock stop than commands one. One of the two fields
		//-- the E->F board revision changed.
		l.add(MicrocodeField.of(14, CKO, 1, 11, 1, values("ON", "OFF")));

		l.add(MicrocodeField.of(15, ABT, 1, 10, 1, values("ALLOW BYTE", "")));
		//-- Value 0 is both bus signals asserted at once, which occurs only in the all-but-zero
		//-- A145 filler microword and is named nowhere.
		l.add(MicrocodeField.of(16, TNS, 3, 8, 2, values(null, "DATO", "DATI", "NONE")));
		l.add(MicrocodeField.of(17, ALG, 3, 6, 2, values("PSW", "SPR", "NULL", "SP")));

		//-- Both read as printed, whatever the signal names suggest - see the class comment. For
		//-- BRG, bit 5 is the 74194's S0 and bit 4 its S1: S1 = L, S0 = H is a shift right.
		l.add(MicrocodeField.of(18, BRG, 0, 4, 2, values("HOLD", "SLEFT", "SRIGHT", "LOAD")));
		l.add(MicrocodeField.of(19, BUT, 017, 0, 4, values(
			"IR-CLK", "INTR", "NON-MOD", "BYTE", "ENOFLO", "MOV", "SWITCHES", "IR-DECODE",
			"SSYNC", "DEST", "UNARY", "JMP/JSR", "SERVICE", "CONST", "INIT", "NON")));

		ARCHITECTURE = new MicrocodeArchitecture("PDP-11/05", l, NXT, BUT, Kd11bMicrocode::verify,
			(mi, field) -> (field.name().equals(ALU) || field.name().equals(MODE)) && takesAluControlFromIr(mi)
				? "the ALU control comes from the instruction register, not from this field"
				: null,
			(mi, field) -> field.name().equals(ALU) ? aluOperation(mi) : field.text(mi.getValue(field)));
	}

	/**
	 * The five-bit code the listing prints and EK-KD11B-MM-001's operation table is keyed by:
	 * the function select above the mode bit.
	 */
	public static int aluCode(MicroInstruction mi) {
		return mi.getValue(ARCHITECTURE.byName(ALU)) << 1 | mi.getValue(ARCHITECTURE.byName(MODE));
	}

	/** The operation the ALU's function select and mode name together, or {@code null} where the table names none. */
	public static String aluOperation(MicroInstruction mi) {
		return ALU_OPERATIONS.get(aluCode(mi));
	}

	/**
	 * Whether this microword takes its ALU control from the instruction register rather than from
	 * its own {@code ALU} field, so that what the field says is not what the machine does.
	 *
	 * <p>Eight microwords in rev F and six in rev E, and not one of them the same one: the E to F
	 * revision moved the set wholesale. Both counts exclude the {@code A145} filler, whose 40
	 * bits are almost entirely zero and which therefore reads as asserting this and every other
	 * active-low line at once.</p>
	 */
	public static boolean takesAluControlFromIr(MicroInstruction mi) {
		return mi.getValue(ARCHITECTURE.byName(AUX)) == 0;
	}
}
