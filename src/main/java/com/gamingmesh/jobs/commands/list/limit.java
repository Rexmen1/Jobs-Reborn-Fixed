package com.gamingmesh.jobs.commands.list;

import java.text.DecimalFormat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.commands.Cmd;
import com.gamingmesh.jobs.container.CurrencyType;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.economy.PaymentData;
import com.gamingmesh.jobs.i18n.Language;

import net.Zrips.CMILib.Locale.LC;
import net.Zrips.CMILib.Messages.CMIMessages;
import net.Zrips.CMILib.Time.CMITimeManager;

public class limit implements Cmd {

    @Override
    public Boolean perform(Jobs plugin, final CommandSender sender, final String[] args) {
        if (args.length != 0 && args.length != 1) {
            return false;
        }

        JobsPlayer JPlayer = null;
        if (args.length >= 1)
            JPlayer = Jobs.getPlayerManager().getJobsPlayer(args[0]);
        else if (sender instanceof Player)
            JPlayer = Jobs.getPlayerManager().getJobsPlayer((Player) sender);

        boolean disabled = true;
        for (CurrencyType type : CurrencyType.values()) {
            if (Jobs.getGCManager().getLimit(type).isEnabled()) {
                disabled = false;
                break;
            }
        }

        if (disabled) {
            Language.sendMessage(sender, "command.limit.output.notenabled");
            return true;
        }

        if (JPlayer == null) {
            if (args.length >= 1)
                CMIMessages.sendMessage(sender, LC.info_NoInformation);
            else if (!(sender instanceof Player))
                Jobs.getCommandManager().sendUsage(sender, "limit");
            return true;
        }

        for (CurrencyType type : CurrencyType.values()) {
            if (!Jobs.getGCManager().getLimit(type).isEnabled())
                continue;
            PaymentData limit = JPlayer.getPaymentLimit();

            if (limit.getLeftTime(type) <= 0) {
                limit.resetLimits(type);
            }

            if (limit.getLeftTime(type) > 0) {
                String typeName = type.getName().toLowerCase();

                Language.sendMessage(sender, "command.limit.output." + typeName + "time", "%time%", CMITimeManager.to24hourShort(limit.getLeftTime(type)));

                if (type == CurrencyType.MONEY && Jobs.getGCManager().useMaxPaymentCurve) {
                    double currentAmount = limit.getAmount(type);
                    double baseLimit = JPlayer.getLimit(type);
                    double factor = Jobs.getGCManager().maxPaymentCurveFactor * 1000.0;
                    int stage = Jobs.getLimitStage(currentAmount, baseLimit, factor);
                    double r = 1.0 - (factor / 100.0);
                    if (stage == Integer.MAX_VALUE) {
                        String absoluteLimit = new DecimalFormat("##.##").format(baseLimit / (1.0 - r));
                        String currentFormatted = new DecimalFormat("##.##").format(currentAmount);
                        Language.sendMessage(sender, "command.limit.output.moneyLimitCurveAbsolute",
                            "[current]", currentFormatted,
                            "%current%", currentFormatted,
                            "[total]", absoluteLimit,
                            "%total%", absoluteLimit);
                    } else {
                        double stageLimit = baseLimit * Math.pow(r, stage);
                        double cumulativeBefore = Jobs.getCumulativeLimitBefore(stage, baseLimit, factor);
                        double stageProgress = currentAmount - cumulativeBefore;
                        
                        String progressFormatted = new DecimalFormat("##.##").format(stageProgress);
                        String totalFormatted = new DecimalFormat("##.##").format(stageLimit);
                        String originalFormatted = new DecimalFormat("##.##").format(baseLimit);
                        String stageStr = String.valueOf(stage);

                        Language.sendMessage(sender, "command.limit.output.moneyLimitCurve",
                            "[current]", progressFormatted,
                            "%current%", progressFormatted,
                            "[total]", totalFormatted,
                            "%total%", totalFormatted,
                            "[original]", originalFormatted,
                            "%original%", originalFormatted,
                            "[stage]", stageStr,
                            "%stage%", stageStr);
                    }
                } else {
                    Language.sendMessage(sender, "command.limit.output." + typeName + "Limit",
                        "%current%", new DecimalFormat("##.##").format(limit.getAmount(type)),
                        "%total%", JPlayer.getLimit(type));
                }
            }
        }
        return true;
    }
}
