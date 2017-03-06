object POSTags {

  // http://nlp.stanford.edu/software/spanish-faq.shtml
  private val adjectives = Set("ao0000", "aq0000")
  private val conjunctions = Set("cc", "cs")
  private val determiners = Set("da0000", "dd0000", "de0000", "di0000", "dn0000", "dp0000", "dt0000")
  private val punctuation = Set("f0", "faa", "fat", "fc", "fd", "fe", "fg", "fh", "fia", "fit", "fp", "fpa", "fpt", "fs", "ft", "fx", "fz")
  private val interjections = Set("i")
  private val nouns = Set("nc00000", "nc0n000", "nc0p000", "nc0s000", "np00000")
  private val pronouns = Set("p0000000", "pd000000", "pe000000", "pi000000", "pn000000", "pp000000", "pr000000", "pt000000", "px000000")
  private val adverbs = Set("rg", "rn")
  private val prepositions = Set("sp000")
  private val verbs = Set("vag0000", "vaic000", "vaif000", "vaii000", "vaip000", "vais000", "vam0000", "van0000", "vap0000", "vasi000",
    "vasp000", "vmg0000", "vmic000", "vmif000", "vmii000", "vmip000", "vmis000", "vmm0000", "vmn0000", "vmp0000",
    "vmsi000", "vmsp000", "vsg0000", "vsic000", "vsif000", "vsii000", "vsip000", "vsis000", "vsm0000", "vsn0000",
    "vsp0000", "vssf000", "vssi000", "vssp000")
  private val dates = Set("w")
  private val numerals = Set("z0", "zm", "zu")
  private val other = Set("word")
  private val undocumented = Set("359000")
  //"vmmp000", "va00000", "vsmp000", "359000", "aqs000", "vmim000", "vmi0000", "vmms000", "vm0p000", "ap0000", "zp", "vq00000", "vm00000", "do0000", "vs00000" // Not documented POS (bug in in v3.7.0)

  // Public values
  val posTags = (adjectives ++ nouns ++ verbs).toSeq
  val posToIndexMap = posTags.zipWithIndex.toMap

}
