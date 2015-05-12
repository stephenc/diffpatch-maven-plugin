Properties props = new Properties();
props.load(new FileInputStream(new File(basedir,"target/classes/first.properties")));
if (!props.getProperty("a").equals("12")) {
    throw new AssertionError("expected a=12");
} 
if (!props.getProperty("b")==null) {
    throw new AssertionError("expected b null");
} 
if (!props.getProperty("c").equals("54")) {
    throw new AssertionError("expected c=54");
} 
if (!props.getProperty("d").equals("43")) {
    throw new AssertionError("expected d=43");
} 
props.clear();
props.load(new FileInputStream(new File(basedir,"target/classes/second.properties")));
if (!props.getProperty("e").equals("67")) {
    throw new AssertionError("expected e=67");
} 
if (!props.getProperty("f")==null) {
    throw new AssertionError("expected f null");
} 
if (!props.getProperty("g").equals("90")) {
    throw new AssertionError("expected g=90");
} 
if (!props.getProperty("h").equals("11")) {
    throw new AssertionError("expected h=11");
} 
