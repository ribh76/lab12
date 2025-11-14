package trees;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;

/**
 * Generic family tree where node "names" can be any type T. A converter
 * function (String->T) is used when reading names from the input file.
 */
public class FamilyTree<T> {

    private static class TreeNode<T> {
        private final T name;
        private TreeNode<T> parent;
        private final ArrayList<TreeNode<T>> children;

        TreeNode(T name) {
            this.name = name;
            this.children = new ArrayList<>();
        }

        T getName() {
            return name;
        }

        void addChild(TreeNode<T> childNode) {
            TreeNode.this.children.add(childNode);
            childNode.parent = TreeNode.this;
        }

        TreeNode<T> getNodeWithName(T targetName) {
            if (name.equals(targetName)) return this;
            for (TreeNode<T> child : children) {
                TreeNode<T> found = child.getNodeWithName(targetName);
                if (found != null) return found;
            }
            return null;
        }

        ArrayList<TreeNode<T>> collectAncestorsToList() {
            ArrayList<TreeNode<T>> ancestors = new ArrayList<>();
            for (TreeNode<T> cur = this.parent; cur != null; cur = cur.parent) {
                ancestors.add(cur);
            }
            return ancestors;
        }

        public String toString() {
            return toStringWithIndent("");
        }

        private String toStringWithIndent(String indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append(name).append("\n");
            String childIndent = indent + "  ";
            for (TreeNode<T> childNode : children)
                sb.append(childNode.toStringWithIndent(childIndent));
            return sb.toString();
        }
    }

    private TreeNode<T> root;
    private final Map<T, TreeNode<T>> nodesByName = new HashMap<>();
    private final java.util.function.Function<String, T> converter;

    @SuppressWarnings("unchecked")
    public FamilyTree() throws IOException, TreeException {
        // default converter assumes T is String
        this((String s) -> (T) s);
    }

    public FamilyTree(java.util.function.Function<String, T> converter) throws IOException, TreeException {
        this.converter = Objects.requireNonNull(converter);

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Family tree text files", "txt");
        File dirf = new File("data");
        if (!dirf.exists()) dirf = new File(".");

        JFileChooser chooser = new JFileChooser(dirf);
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) System.exit(1);
        File treeFile = chooser.getSelectedFile();

        try (FileReader fr = new FileReader(treeFile); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) addLine(line);
        }
    }

    // Line format is "parent:child1,child2 ..."
    private void addLine(String line) throws TreeException {
        if (line.isEmpty() || line.startsWith("#")) return;
        int colonIndex = line.indexOf(':');
        if (colonIndex <= 0 || colonIndex == line.length() - 1) {
            throw new TreeException("Bad line (no colon or no children): " + line);
        }
        String parentNameStr = line.substring(0, colonIndex).trim();
        String[] kids = line.substring(colonIndex + 1).split(",");

        if (parentNameStr.isEmpty()) throw new TreeException("Empty parent name: " + line);

        T parentName = converter.apply(parentNameStr);

        TreeNode<T> parent = nodesByName.get(parentName);
        if (parent == null) {
            parent = new TreeNode<>(parentName);
            nodesByName.put(parentName, parent);
            if (root == null) root = parent; // first seen becomes root; may be updated later
        }

        for (String kidRaw : kids) {
            String childNameStr = kidRaw.trim();
            if (childNameStr.isEmpty()) continue;
            T childName = converter.apply(childNameStr);
            TreeNode<T> child = nodesByName.get(childName);
            if (child == null) {
                child = new TreeNode<>(childName);
                nodesByName.put(childName, child);
            }
            if (child.parent != null && child.parent != parent) {
                throw new TreeException("Child " + childNameStr + " already has a different parent " + child.parent.getName());
            }
            parent.addChild(child);
        }

        // Recompute root: the unique node with no parent if available
        for (TreeNode<T> n : nodesByName.values()) {
            if (n.parent == null) {
                root = n;
                break;
            }
        }
    }

    private ArrayList<TreeNode<T>> ancestorsOf(TreeNode<T> node) {
        ArrayList<TreeNode<T>> chain = new ArrayList<>();
        for (TreeNode<T> cur = node; cur != null; cur = cur.parent) {
            chain.add(cur);
        }
        return chain;
    }

    // Returns the "deepest" node that is an ancestor of the node named name1, and
    // also is an
    // ancestor of the node named name2.
    TreeNode<T> getMostRecentCommonAncestor(T name1, T name2) throws TreeException {
        TreeNode<T> n1 = nodesByName.get(name1);
        TreeNode<T> n2 = nodesByName.get(name2);
        if (n1 == null) throw new TreeException("Node not found: " + name1);
        if (n2 == null) throw new TreeException("Node not found: " + name2);

        ArrayList<TreeNode<T>> ancestorsOf1 = ancestorsOf(n1);
        ArrayList<TreeNode<T>> ancestorsOf2 = ancestorsOf(n2);

        for (TreeNode<T> n : ancestorsOf1) {
            if (ancestorsOf2.contains(n)) return n;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Family Tree:\n\n" + root;
    }

    public static void main(String[] args) {
        try {
            FamilyTree<String> tree = new FamilyTree<>();
            System.out.println("Tree:\n" + tree + "\n**************\n");
            TreeNode<String> ancestor = tree.getMostRecentCommonAncestor("Bilbo", "Frodo");
            System.out.println("Most recent common ancestor of Bilbo and Frodo is " + ancestor.getName());
        } catch (IOException x) {
            System.out.println("IO trouble: " + x.getMessage());
        } catch (TreeException x) {
            System.out.println("Input file trouble: " + x.getMessage());
        }
    }
}
