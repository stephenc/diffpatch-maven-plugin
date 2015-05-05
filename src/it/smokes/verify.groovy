Properties props = new Properties();
props.load(new FileInputStream(new File(basedir,"target/classes/simple.properties")));
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
