package cpacgen;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import java.util.ArrayList;
import java.util.List;

public class Plan {
    private List<Step> start = new ArrayList<>();
    private List<Step> all = new ArrayList<>();
    private List<Step> head = new ArrayList<>();

    public Plan(List<Goal> init, String comment){
        for (Goal g: init){
            Goal.Pair p = new Goal.Pair(null, g, new Transformation.Null());
            Step s = new Step(p, comment);
            start.add(s);
        }
        all.addAll(start);
        head.addAll(start);
    }

    private Plan(List<Step> init){
        start.addAll(init);
    }

    public Plan newAdd(Goal.Pair transformation, String comment) {
        Plan out = new Plan(start);
        out.all.addAll(all);
        out.head.addAll(head);
        Step newStep = new Step(transformation, comment);
        for(Step s: out.all){
            for(Goal l: s.goalPair.lowers){
                if(l.same(transformation.upper)){
                    newStep.links.add(s);
                    out.head.remove(s);
                }
            }
        }
        out.all.add(newStep);
        out.head.add(newStep);
        return out;
    }

    private static class Step {
        private final String comment;
        Goal.Pair goalPair;
        List<Step> links;
        private String name;

        private Step(Goal.Pair t, String comment) {
            goalPair = t;
            this.comment = comment;
            links = new ArrayList<>();
        }


        @Override
        public String toString() {
            return "Step{" +
                    "goalPair=" + goalPair.toString() +
                    ", " + comment +
                    '}';
        }

        public String toStringN() {
            return name +" " + goalPair.toStringN() +
                    "\n" + comment;
        }

        private void addEdgesToGraph(Graph graph){
            Node node = graph.getNode(name);
            node.addAttribute("ui.label", toStringN());

            node.setAttribute("ui.class", "basicBlock");

            for(Step s: links) {
                graph.addEdge(name + "-" +s.name, name, s.name, true);
                //s.addEdgesToGraph(graph);
            }
        }
    }

    public Double cost(){
        return all.stream().mapToDouble(x -> x.goalPair.transformation.cost()).sum();
    }

    public int depth(){
        return all.size()-1;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("plan:\n");
        for (int i = 0; i<all.size(); i++) {
            sb.append(i+1);
            sb.append(": ");
            sb.append(all.get(i).toString());
            sb.append('\n');
        }
        return sb.toString();
    }


    public void display(){
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        Graph graph = new SingleGraph("Plan");

        graph.setStrict(true);
        String styleSheet =
                "node { stroke-mode: plain; " +
                        "fill-color: black; " +
                        "shape: rounded-box; " +
                        "size: 50px, 50px; " +
                        "text-size:24; " +
                        "padding: 4px, 4px; " +
                        "text-alignment: at-right;" +
                        "text-padding: 3px;" +
                        "text-background-mode: plain;" +
                        "}" +
                "edge { arrow-shape: arrow; " +
                        "arrow-size: 10px, 10px; " +
                        "}";
        graph.addAttribute("ui.stylesheet",styleSheet);
        for (int i = 0; i < all.size(); i++) {
            all.get(i).name = "N"+i;
            graph.addNode(all.get(i).name);
        }
        for (Step s: all){
            s.addEdgesToGraph(graph);
        }

        Viewer viewer = graph.display();
        viewer.enableAutoLayout();
        //viewer.addDefaultView(false).getCamera().setViewPercent(0.5);



    }




}
