package ca.cutterslade.util.processpool;

import java.io.Serializable;

interface ProcessCommand extends Serializable {
  void execute(ProcessContext context);
}
