(define (make-circle radius)

  (lambda (operation)
  
    (cond

      ((eq? operation 'area)
       (* 3.14159 radius radius))

      ((eq? operation 'perimeter)
       (* 2 3.14159 radius))

      (else

       "Unknown operation"))))

(define (make-square side)

  (lambda (operation)
    (cond

      ((eq? operation 'area)
       (* side side))

      ((eq? operation 'perimeter)
       (* 4 side))

      (else

       "Unknown operation"))))

(define circle (make-circle 5))
(define square (make-square 4))

(display (circle 'area))

(newline)

(display (square 'perimeter))

(newline)