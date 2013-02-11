package ca.cutterslade.util.processpool;

interface ProcessContext {
  void killProcess();

  void setResult(Object result);
}
