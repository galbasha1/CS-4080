#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct Node {

    char *data;
    struct Node *prev;
    struct Node *next;

} Node;

void insert(Node **head, char *text) {

    Node *newNode = malloc(sizeof(Node));

    newNode->data = malloc(strlen(text) + 1);
    strcpy(newNode->data, text);


    newNode->prev = NULL;
    newNode->next = NULL;

    if (*head == NULL) {

        *head = newNode;
        return;
    }

    Node *temp = *head;

    while (temp->next != NULL) {

        temp = temp->next;
    }

    temp->next = newNode;
    newNode->prev = temp;
}

Node *find(Node *head, char *text) {

    Node *temp = head;

    while (temp != NULL) {

        if (strcmp(temp->data, text) == 0) {
            return temp;
        }

        temp = temp->next;
    }

    return NULL;
}

void delete(Node **head, char *text) {

    Node *temp = find(*head, text);

    if (temp == NULL) {
        printf("Item not found.\n");
        return;
    }

    if (temp->prev != NULL) {

        temp->prev->next = temp->next;
    } else {

        *head = temp->next;
    }

    if (temp->next != NULL) {

        temp->next->prev = temp->prev;
    }

    free(temp->data);
    free(temp);
}

void printList(Node *head) {

    Node *temp = head;

    while (temp != NULL) {

        printf("%s", temp->data);

        if (temp->next != NULL) {

            printf(" <-> ");
        }

        temp = temp->next;
    }

    printf("\n");

}

int main() {
    Node *head = NULL;

    insert(&head, "Apple");
    insert(&head, "Banana");
    insert(&head, "Orange");

    printf("List: ");
    printList(head);

    Node *result = find(head, "Banana");

    if (result != NULL) {
        
        printf("Found: %s\n", result->data);
    }

    delete(&head, "Banana");

    printf("After deleting Banana: ");
    printList(head);

    return 0;
}