package bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bot.entity.FightingStrength;

@Repository
public interface FightingStrengthRepository extends JpaRepository<FightingStrength, Integer>{

}
