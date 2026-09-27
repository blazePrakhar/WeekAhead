import { motion } from "motion/react";

function PageTransition({ children, className = "" }) {
  return (
    <motion.div
      className={className}
      initial={{
        opacity: 0,
        y: 10,
      }}
      animate={{
        opacity: 1,
        y: 0,
      }}
      transition={{
        duration: 0.28,
        ease: [0.2, 0.8, 0.2, 1],
      }}
      style={{
        width: "100%",
      }}
    >
      {children}
    </motion.div>
  );
}

export default PageTransition;
