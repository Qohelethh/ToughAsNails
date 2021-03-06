package toughasnails.handler;

import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import toughasnails.thirst.ThirstStats;

public class ThirstStatHandler
{
    private UUID playerID;
    private double startX;
    private double startY;
    private double startZ;
    private float exhaustion;
    
    @SubscribeEvent
    public void onPlayerJump(LivingJumpEvent event)
    {
        World world = event.entity.worldObj;

        if (!world.isRemote)
        {
            if (event.entity instanceof EntityPlayer)
            {
                EntityPlayer player = (EntityPlayer)event.entity;
                ThirstStats thirstStats = (ThirstStats)player.getExtendedProperties("thirst");

                if (player.isSprinting())
                {
                    thirstStats.addExhaustion(0.8F);
                }
                else
                {
                    thirstStats.addExhaustion(0.2F);
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event)
    {
        World world = event.entity.worldObj;

        if (!world.isRemote && event.ammount != 0.0F)
        {
            if (event.entity instanceof EntityPlayer)
            {
                EntityPlayer player = (EntityPlayer)event.entity;
                ThirstStats thirstStats = (ThirstStats)player.getExtendedProperties("thirst");
                
                //Uses hunger values for now, may change in the future
                thirstStats.addExhaustion(event.source.getHungerDamage());
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerAttackEntity(AttackEntityEvent event)
    {
        World world = event.entity.worldObj;
        Entity target = event.target;
        
        if (!world.isRemote)
        {
            EntityPlayer player = event.entityPlayer;
            
            if (target.canAttackWithItem())
            {
                if (!target.hitByEntity(player))
                {
                    float attackDamage = (float)player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
                    float weaponAttackDamage = 0.0F;
                    
                    if (target instanceof EntityLivingBase)
                    {
                        weaponAttackDamage = EnchantmentHelper.func_152377_a(player.getHeldItem(), ((EntityLivingBase)target).getCreatureAttribute());
                    }
                    else
                    {
                        weaponAttackDamage = EnchantmentHelper.func_152377_a(player.getHeldItem(), EnumCreatureAttribute.UNDEFINED);
                    }
                    
                    if (attackDamage > 0.0F || weaponAttackDamage > 0.0F)
                    {
                        boolean flag = player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder() && !player.isInWater() && !player.isPotionActive(Potion.blindness) && player.ridingEntity == null && target instanceof EntityLivingBase;

                        if (flag && attackDamage > 0.0F)
                        {
                            attackDamage *= 1.5F;
                        }

                        attackDamage += weaponAttackDamage;
                        
                        boolean canAttack = target.attackEntityFrom(DamageSource.causePlayerDamage(player), attackDamage);
                        
                        if (canAttack)
                        {
                            //The only part of this method that is new - the rest is recreating the surrounding circumstances in attackTargetEntityWithCurrentItem
                            ThirstStats thirstStats = (ThirstStats)player.getExtendedProperties("thirst");
                            
                            thirstStats.addExhaustion(0.3F);
                        }
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event)
    {
        World world = event.world;
        EntityPlayer player = event.getPlayer();
        BlockPos pos = event.pos;
        IBlockState state = event.state;
        
        if (!world.isRemote && !player.capabilities.isCreativeMode)
        {
            boolean canHarvestBlock = state.getBlock().canHarvestBlock(world, pos, player);
            
            if (canHarvestBlock)
            {
                //The only part of this method that is new - the rest is recreating the surrounding circumstances in func_180237_b
                ThirstStats thirstStats = (ThirstStats)player.getExtendedProperties("thirst");
                
                thirstStats.addExhaustion(0.025F);
            }
        }
    }
}
