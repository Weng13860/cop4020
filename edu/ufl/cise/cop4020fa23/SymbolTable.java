package edu.ufl.cise.cop4020fa23;

import java.util.HashMap;
import java.util.Stack;

// function names from PowerPoint/Slack
// K for key, V for value until I figure out the correct Values
public class SymbolTable<K, V> {
    private Stack<HashMap<K, V>> tables;

    public SymbolTable() {
        tables = new Stack<>();
        tables.push(new HashMap<>());  // Initialize with a global scope.
    }

    public V lookup(K key) {
        for (int i = tables.size() - 1; i >= 0; i--) {
            if (tables.get(i).containsKey(key)) {
                return tables.get(i).get(key);
            }
        }
        return null;
    }

    public void insert(K key, V value) {
        tables.peek().put(key, value);
    }

    public void enterScope() {
        tables.push(new HashMap<>());
    }

    public void leaveScope() {
        if (tables.size() == 1) {
            throw new IllegalStateException("Cannot leave the global scope");
        }
        tables.pop();
    }

    public static void main(String[] args) {
        SymbolTable<String, Integer> symbolTable = new SymbolTable<>();

        symbolTable.insert("x", 1);
        System.out.println(symbolTable.lookup("x"));

        symbolTable.enterScope();
        symbolTable.insert("x", 2);
        System.out.println(symbolTable.lookup("x"));

        symbolTable.leaveScope();
        System.out.println(symbolTable.lookup("x"));
    }
}
