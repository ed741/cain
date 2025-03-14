package uk.co.edstow.cain.scamp5.output;

import java.util.ArrayList;


public class OutputCode {
    private final ArrayList<String> lines = new ArrayList<>();
    private int ops = 0;
    private int selects = 0;
    private int comments = 0;
    private int other = 0;

    public OutputCode addOp(String opString){
        lines.add(opString);
        ops++;
        return this;
    }
    public OutputCode addSelect(String opString){
        lines.add(opString);
        ops++;
        selects++;
        return this;
    }
    public OutputCode addComment(String commentString){
        lines.add(commentString);
        comments++;
        return this;
    }
    public OutputCode addOther(String otherString){
        lines.add(otherString);
        other++;
        return this;
    }
    public OutputCode addOutput(OutputCode outputCode){
        lines.addAll(outputCode.lines);
        ops += outputCode.ops;
        selects += outputCode.selects;
        comments += outputCode.comments;
        other += outputCode.other;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {sb.append(line).append('\n');}
        return sb.toString();
    }

    public String getString() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {sb.append(line).append('\n');}
        sb.append(String.format("/* ops: %d, selects: %d, comments: %d, other: %d  */", ops, selects, comments, other));
        return sb.toString();
    }


}
