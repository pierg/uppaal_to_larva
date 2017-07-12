
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author chalmers-internship
 */
public class UppaalTemplate {

    private final String _property;

    private final List<State> _states;
    private final List<Transition> _transitions;

    public UppaalTemplate(String property) {
        _property = property;
        _states = new ArrayList<>();
        _transitions = new ArrayList<>();
    }

    public void addState(State state) {
        _states.add(state);
    }

    public void addTransition(Transition transition) {
        _transitions.add(transition);
    }

    public List<State> getStates() {
        return _states;
    }

    public List<Transition> getTransitions() {
        return _transitions;
    }

    public void retrieveStatesFromXML(Element template) {
        NodeList locations = template.getElementsByTagName("location");

        for (int i = 0; i < locations.getLength(); i++) {
            Element location = (Element) (locations.item(i));
            String state = location.getTextContent();
            _states.add(new State(location.getAttribute("id"), state.trim()));
        }

        _states.add(new State("0", "default"));
    }

    public void retrieveTransitionsFromXML(Element template) {
        NodeList transitionNodes = template.getElementsByTagName("transition");

        for (int i = 0; i < transitionNodes.getLength(); i++) {
            Element transitionNode = (Element) (transitionNodes.item(i));
            String idFrom = ((Element) (transitionNode.getElementsByTagName("source").item(0))).getAttribute("ref");
            String idTo = ((Element) (transitionNode.getElementsByTagName("target").item(0))).getAttribute("ref");
            NodeList labels = transitionNode.getElementsByTagName("label");
            _transitions.add(getTransitionFromXMLNode(_states, i, idFrom, idTo, labels));
        }
    }

    public String getLARVAStatesDeclaration() {
        String stateDeclaration = "";
        stateDeclaration = _states.stream().filter((state) -> (!state._name.equals("start"))).map((state) -> state + " ").reduce(stateDeclaration, String::concat);

        return stateDeclaration;
    }

    public String getLARVATransitionsCode() {
        String transitionsCode = "";
        String regex = "([a-zA-Z]|\\d|\\(|\\))+(?=( *,*))";
        Pattern pattern = Pattern.compile(regex);

        for (Transition tr : _transitions) {
            transitionsCode += "\t\t\t" + tr._from + "->" + tr._to;

            if (!tr._guard.equals("") && !tr._guard.equals("none")) {
                transitionsCode += "[rlevent\\\\\\\\\\EchoServer." + tr._guard;
            } else {
                transitionsCode += "[rlevent\\\\\\\\\\";
            }

            if (!tr._assignment.equals("") && !tr._assignment.equals("none")) {
                transitionsCode += "\\\\\\\\\\";
                Matcher matcher = pattern.matcher(tr._assignment);
                while (matcher.find()) {
                       transitionsCode += "EchoServer."+ matcher.group() + ";";
                }
                transitionsCode += "EchoServer.propertyChecked();EchoServer.c_" + _property + "_" + tr._to._name + "++;]\n";
            } else {
                transitionsCode += "\\\\\\\\\\EchoServer.propertyChecked();EchoServer.c_" + _property + "_" + tr._to._name + "++;]\n";
            }

        }

        return transitionsCode;
    }

    public String getLARVAResetTransitionsCode() {
        String statement = "->start[reset()\\\\\\\\\\\\\\\\\\\\EchoServer.tot_reward = 0;EchoServer.resetAgent();]\n";
        String resetTransition = "start" + statement;

        resetTransition = _states.stream().map((state) -> "\t\t\t" + state + statement).reduce(resetTransition, String::concat);

        return resetTransition;
    }

    public String getLARVADefaultTransitionsCode() {
        String statement = "->default[rlevent()\\\\\\\\\\\\\\\\\\\\EchoServer.tot_reward = 0;EchoServer.resetAgent();]\n";
        String defaultTransition = "";

        defaultTransition = _states.stream().map((state) -> "\t\t\t" + state + statement).reduce(defaultTransition, String::concat);

        return defaultTransition;
    }

    public String getPropertyName() {
        return _property;
    }

    private Transition getTransitionFromXMLNode(List<State> states, int i, String idFrom, String idTo, NodeList labels) {
        State from = new State();
        State to = new State();
        String guard = new String();
        String assignment = new String();
        for (int j = 0; j < labels.getLength(); j++) {
            Element label = (Element) (labels.item(j));
            if (label.getAttribute("kind").equals("guard")) {
                guard = label.getTextContent();
            } else if (label.getAttribute("kind").equals("assignment")) {
                assignment = label.getTextContent();
            }
        }

        for (State state : states) {
            if (state._id.equals(idFrom)) {
                from = state;
            }

            if (state._id.equals(idTo)) {
                to = state;
            }
        }
        return new Transition(from, to, guard, assignment);
    }

}
