package trees;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class FamilyTree {

    private static class TreeNode {
        private String name;
        private TreeNode parent;
        private ArrayList<TreeNode> children;

        TreeNode(String name) {
            this.name = name;
            children = new ArrayList<>();
        }

        String getName() {
            return name;
        }

        void addChild(TreeNode childNode) {
            TreeNode.this.children.add(childNode);
            childNode.parent = TreeNode.this;
            // Add childNode to this node's children list. Also
            // set childNode's parent to this node.
        }

        // Searches subtree at this node for a node
        // with the given name. Returns the node, or null if not found.
        TreeNode getNodeWithName(String targetName) {
            // Does this node have the target name?
            if (name.equals(targetName))
                return this;   
                    
            // No, recurse. Check all children of this node.
            for (TreeNode child: children)
            {
                // If child.getNodeWithName(targetName) returns a non-null node,
                // then that's the node we're looking for. Return it.
                if ( child.getNodeWithName(targetName) != null )   // Fill in condition
                {
                    return child.getNodeWithName(targetName);
                }
            }
            
            // Not found anywhere.
            return null;
        }

        // Returns a list of ancestors of this TreeNode, starting with this node’s
        // parent and
        // ending with the root. Order is from recent to ancient.
            ArrayList<TreeNode> collectAncestorsToList() {
            ArrayList<TreeNode> ancestors = new ArrayList<>();

            // ????? Collect ancestors of this TreeNode into the array list. HINT: going up
            // the nodes of a tree is like traversing a linked list. If that isn’t clear,
            // draw a tree, mark any leaf node, and then mark its ancestors in order from
            // recent to ancient. Expect a question about this on the final exam.

            return ancestors;
        }

        public String toString() {
            return toStringWithIndent("");
        }

        private String toStringWithIndent(String indent) {
            String s = indent + name + "\n";
            indent += "  ";
            for (TreeNode childNode : children)
                s += childNode.toStringWithIndent(indent);
            return s;
        }
    }

    private TreeNode root;
    private Map<String, TreeNode> nodesByName = new HashMap<>();

    //
    // Displays a file browser so that user can select the family tree file.
    //
    public FamilyTree() throws IOException, TreeException {
        // User chooses input file. This block doesn't need any work.
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Family tree text files", "txt");
        File dirf = new File("data");
        if (!dirf.exists()) dirf = new File(".");

        JFileChooser chooser = new JFileChooser(dirf);
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) System.exit(1);
        File treeFile = chooser.getSelectedFile();

        // Parse the input file. Create a FileReader that reads treeFile. Create a BufferedReader
        // that reads from the FileReader.
        FileReader fr = new FileReader(treeFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        while ((line = br.readLine()) != null)
            addLine(line);
        br.close();
        fr.close();
    }

    //
    // Line format is "parent:child1,child2 ..."
    // Throws TreeException if line is illegal.
    //
    private void addLine(String line) throws TreeException
    { 
        if (line.isEmpty() || line.startsWith("#")) return;
        int colonIndex = line.indexOf(':');
        if (colonIndex <= 0 || colonIndex == line.length()-1) {
            throw new TreeException("Bad line (no colon or no children): " + line);
        }
        String parentName = line.substring(0, colonIndex).trim();
        String[] kids = line.substring(colonIndex+1).split(",");

        if (parentName.isEmpty()) throw new TreeException("Empty parent name: " + line);

        TreeNode parent = nodesByName.get(parentName);
        if (parent == null) {
            parent = new TreeNode(parentName);
            nodesByName.put(parentName, parent);
            if (root == null) root = parent; // first seen becomes root; may be updated later
        }

        for (String kidRaw : kids) {
            String childName = kidRaw.trim();
            if (childName.isEmpty()) continue;
            TreeNode child = nodesByName.get(childName);
            if (child == null) {
                child = new TreeNode(childName);
                nodesByName.put(childName, child);
            }
            if (child.parent != null && child.parent != parent) {
                throw new TreeException("Child " + childName + " already has a different parent " + child.parent.getName());
            }
            parent.addChild(child);
        }

        // Recompute root: the unique node with no parent if available
        for (TreeNode n : nodesByName.values()) {
            if (n.parent == null) { root = n; break; }
        }
    }

    private ArrayList<TreeNode> ancestorsOf(TreeNode node) {
        ArrayList<TreeNode> chain = new ArrayList<>();
        for (TreeNode cur = node; cur != null; cur = cur.parent) {
            chain.add(cur);
        }
        return chain;
    }
    // Returns the "deepest" node that is an ancestor of the node named name1, and
    // also is an
    // ancestor of the node named name2.
    //
    // "Depth" of a node is the "distance" between that node and the root. The depth
    // of the root is 0. The
    // depth of the root's immediate children is 1, and so on.
    //
    TreeNode getMostRecentCommonAncestor(String name1, String name2) throws TreeException
    {
        // Get nodes for input names.
        TreeNode n1 = nodesByName.get(name1);
        TreeNode n2 = nodesByName.get(name2);        // node whose name is name1
        if (n1 == null)
            throw new TreeException("Node not found: " + name1);
        if (n2 == null)
            throw new TreeException("Node not found: " + name2);
        
        // Get ancestors of node1 and node2.
        ArrayList<TreeNode> ancestorsOf1 = ancestorsOf(n1);
        ArrayList<TreeNode> ancestorsOf2 = ancestorsOf(n2);
        
        // Check members of ancestorsOf1 in order until you find a node that is also
        // an ancestor of 2. 
        for (TreeNode n : ancestorsOf1)
            if (ancestorsOf2.contains(n1))
                return n1;
        
        // No common ancestor.
        return null;
    }
    @Override
    public String toString() {
        return "Family Tree:\n\n" + root;
    }

    public static void main(String[] args) {
        try {
            FamilyTree tree = new FamilyTree();
            System.out.println("Tree:\n" + tree + "\n**************\n");
            TreeNode ancestor = tree.getMostRecentCommonAncestor("Bilbo", "Frodo");
            System.out.println("Most recent common ancestor of Bilbo and Frodo is " + ancestor.getName());
        } catch (IOException x) {
            System.out.println("IO trouble: " + x.getMessage());
        } catch (TreeException x) {
            System.out.println("Input file trouble: " + x.getMessage());
        }
    }
}
